use std::collections::{HashMap, HashSet};
use std::{char, usize};

use crate::letter::{Format, LetterMsgIn};
use crate::{
    config::{APP_ID, PROFILE},
    letter::LetterMsgOut,
};
use crate::{letter::Letter, modals::about::AboutDialog};
use gtk::prelude::{
    ApplicationExt, ApplicationWindowExt, ButtonExt, GtkWindowExt, OrientableExt, SettingsExt,
    WidgetExt,
};
use gtk::{gio, glib};
use rand::seq::IteratorRandom;
use relm4::actions::AccelsPlus;
use relm4::gtk::glib::Propagation;
use relm4::gtk::EventControllerKey;
use relm4::{
    actions::{RelmAction, RelmActionGroup},
    adw,
    factory::FactoryVecDeque,
    gtk::{self, prelude::GridExt},
    main_application, Component, ComponentController, ComponentParts, ComponentSender, Controller,
    SimpleComponent,
};

static TRIES: usize = 6;
static WORDS_FILE: &str = include_str!("../data/resources/wordlists/words.txt");

pub(super) struct App {
    letters: FactoryVecDeque<Letter>,
    about_dialog: Controller<AboutDialog>,
    selected_letter: usize,
    word: String,
    width: usize,
    attempts: usize,
    allowed_words: HashSet<&'static str>,
}

#[derive(Debug)]
pub(super) enum AppMsg {
    SelectField(usize),
    StartNewGame,
    EnterLetter(char),
    Enter,
    Delete,
    Backspace,
    Quit,
}

relm4::new_action_group!(pub(super) WindowActionGroup, "win");
relm4::new_stateless_action!(PreferencesAction, WindowActionGroup, "preferences");
relm4::new_stateless_action!(pub(super) ShortcutsAction, WindowActionGroup, "show-help-overlay");
relm4::new_stateless_action!(AboutAction, WindowActionGroup, "about");
relm4::new_stateless_action!(EnterAction, WindowActionGroup, "enter");

#[relm4::component(pub)]
impl SimpleComponent for App {
    type Init = ();
    type Input = AppMsg;
    type Output = ();
    type Widgets = AppWidgets;

    menu! {
        primary_menu: {
            section! {
                "_Preferences" => PreferencesAction,
                "_Keyboard" => ShortcutsAction,
                "_About Wordle" => AboutAction,
            }
        }
    }

    view! {
        main_window = adw::ApplicationWindow::new(&main_application()) {
            set_visible: true,

            connect_close_request[sender] => move |_| {
                sender.input(AppMsg::Quit);
                glib::Propagation::Stop
            },

            #[wrap(Some)]
            set_help_overlay: shortcuts = &gtk::Builder::from_resource(
                    "/com/gitlab/tronta/wordle/gtk/help-overlay.ui"
                )
                .object::<gtk::ShortcutsWindow>("help_overlay")
                .unwrap() -> gtk::ShortcutsWindow {
                    set_transient_for: Some(&main_window),
                    set_application: Some(&main_application()),
            },

            add_css_class?: if PROFILE == "Devel" {
                    Some("devel")
                } else {
                    None
                },

            gtk::Box {
                set_orientation: gtk::Orientation::Vertical,

                adw::HeaderBar {
                    pack_start = &gtk::Button {
                        set_label: "Start",
                        set_can_focus: false,
                        connect_clicked => AppMsg::StartNewGame,
                    },
                    pack_end = &gtk::MenuButton {
                        set_icon_name: "open-menu-symbolic",
                        set_menu_model: Some(&primary_menu),
                    }
                },

                #[local_ref]
                letter_grid -> gtk::Grid {
                    set_orientation: gtk::Orientation::Horizontal,
                    set_column_spacing: 0,
                    set_row_spacing: 0,
                }
            }

        }
    }

    fn init(
        _init: Self::Init,
        root: Self::Root,
        sender: ComponentSender<Self>,
    ) -> ComponentParts<Self> {
        let about_dialog = AboutDialog::builder()
            .transient_for(&root)
            .launch(())
            .detach();

        let letters =
            FactoryVecDeque::builder()
                .launch_default()
                .forward(sender.input_sender(), |msg| match msg {
                    LetterMsgOut::Selected(index) => AppMsg::SelectField(index.current_index()),
                });

        let allowed_words = WORDS_FILE.lines().collect();

        let model = Self {
            about_dialog,
            letters,
            selected_letter: 0,
            word: String::new(),
            attempts: 0,
            allowed_words,
            width: 0,
        };

        root.add_controller(keyboard_events_controller(sender.clone()));

        let letter_grid = model.letters.widget();
        let widgets = view_output!();

        register_actions(sender.clone(), &widgets, &model);

        widgets.load_window_size();

        sender.input(AppMsg::StartNewGame);

        ComponentParts { model, widgets }
    }

    fn update(&mut self, message: Self::Input, sender: ComponentSender<Self>) {
        let mut letters_guard = self.letters.guard();

        let selected = self.selected_letter;

        let mut select_field = |index: usize| {
            letters_guard.send(self.selected_letter, LetterMsgIn::SetSelected(false));

            letters_guard.send(index, LetterMsgIn::SetSelected(true));
            self.selected_letter = index;
        };

        match message {
            AppMsg::Quit => main_application().quit(),
            AppMsg::SelectField(index) => select_field(index),
            AppMsg::StartNewGame => {
                self.word = pick_random_word(&self.allowed_words);
                self.width = self.word.chars().count();
                self.attempts = 0;
                self.selected_letter = 0;

                letters_guard.clear();
                for i in 0..self.width * TRIES {
                    if i < self.width {
                        letters_guard.push_back((self.width, Format::Editable));
                    } else {
                        letters_guard.push_back((self.width, Format::NoMatch));
                    }
                }
            }
            AppMsg::EnterLetter(c) => {
                letters_guard.send(
                    selected,
                    LetterMsgIn::SetContent(Some(c.to_uppercase().to_string())),
                );

                if selected < ((self.attempts + 1) * self.word.chars().count()) - 1 {
                    let new_selected_letter = selected + 1;
                    select_field(new_selected_letter);
                }
            }
            AppMsg::Delete => letters_guard.send(selected, LetterMsgIn::SetContent(None)),
            AppMsg::Backspace => {
                let width = self.word.chars().count();
                if selected == (self.attempts + 1) * width - 1 {
                    if !letters_guard.get(selected).unwrap().value.is_empty() {
                        sender.input(AppMsg::Delete);
                        return;
                    }
                }
                if selected > (self.attempts * width) {
                    let new_selected_letter = selected - 1;
                    select_field(new_selected_letter);
                    letters_guard.send(new_selected_letter, LetterMsgIn::SetContent(None))
                }
            }
            AppMsg::Enter => {
                let mut r = String::new();
                let width = self.word.chars().count();

                for i in 0..width {
                    let c_u = &letters_guard.get(self.attempts * width + i).unwrap().value;
                    r.push_str(&c_u);
                }
                if !self.allowed_words.contains(r.to_uppercase().as_str()) {
                    return;
                }

                let mut left_letters = HashMap::new();
                let mut correct_letters = HashSet::new();
                for (i, c) in self.word.chars().enumerate() {
                    let c_u = &letters_guard.get(self.attempts * width + i).unwrap().value;
                    if c_u == "" {
                        return;
                    }
                    r.push_str(&c_u);
                    if c.to_string() == *c_u {
                        letters_guard.send(
                            self.attempts * width + i,
                            LetterMsgIn::SetFormat(Format::ExactMatch),
                        );
                        correct_letters.insert(i);
                    } else {
                        left_letters
                            .entry(c.to_string())
                            .and_modify(|c| *c += 1)
                            .or_insert(1);
                    };
                }

                for (i, _) in &mut self.word.chars().enumerate() {
                    if correct_letters.contains(&i) {
                        continue;
                    }
                    let c_u = &letters_guard.get(self.attempts * width + i).unwrap().value;
                    if let Some(number) = left_letters.get_mut(c_u) {
                        if *number > 0 {
                            letters_guard.send(
                                self.attempts * width + i,
                                LetterMsgIn::SetFormat(Format::Match),
                            );
                            *number -= 1;
                            continue;
                        }
                    }
                    letters_guard.send(
                        self.attempts * width + i,
                        LetterMsgIn::SetFormat(Format::NoMatch),
                    )
                }

                self.attempts += 1;

                if self.attempts > 5 {
                    println!("Solution: {}", self.word);
                    sender.input(AppMsg::StartNewGame);
                    return;
                }

                for i in 0..width {
                    letters_guard.send(
                        self.attempts * width + i,
                        LetterMsgIn::SetFormat(Format::Editable),
                    )
                }
                sender.input(AppMsg::SelectField(self.attempts * width));
            }
        }
    }

    fn shutdown(&mut self, widgets: &mut Self::Widgets, _output: relm4::Sender<Self::Output>) {
        widgets.save_window_size().unwrap();
    }
}

fn pick_random_word(words: &HashSet<&str>) -> String {
    words
        .iter()
        .choose(&mut rand::thread_rng())
        .unwrap()
        .to_string()
}

fn keyboard_events_controller(sender: ComponentSender<App>) -> EventControllerKey {
    let controller = EventControllerKey::new();
    // Connect to the key-pressed signal to handle key presses
    controller.connect_key_pressed(move |_, keyval, _, _| {
        if let Some(c) = keyval.to_unicode() {
            match c {
                '\u{8}' => sender.input(AppMsg::Backspace),
                '\u{7f}' => sender.input(AppMsg::Delete),
                c if c.is_alphabetic() => sender.input(AppMsg::EnterLetter(c)),
                _ => (),
            }
            Propagation::Stop
        } else {
            Propagation::Proceed
        }
    });
    controller
}

fn register_actions(sender: ComponentSender<App>, widgets: &AppWidgets, model: &App) {
    let mut actions = RelmActionGroup::<WindowActionGroup>::new();

    let enter_action = {
        RelmAction::<EnterAction>::new_stateless(move |_| {
            sender.input(AppMsg::Enter);
        })
    };

    let app = relm4::main_application();
    app.set_accelerators_for_action::<EnterAction>(&["<Control>Return"]);

    let shortcuts_action = {
        let shortcuts = widgets.shortcuts.clone();
        RelmAction::<ShortcutsAction>::new_stateless(move |_| {
            shortcuts.present();
        })
    };

    let about_action = {
        let sender = model.about_dialog.sender().clone();
        RelmAction::<AboutAction>::new_stateless(move |_| {
            sender.send(()).unwrap();
        })
    };

    actions.add_action(shortcuts_action);
    actions.add_action(about_action);
    actions.add_action(enter_action);
    actions.register_for_widget(&widgets.main_window);
}

impl AppWidgets {
    fn save_window_size(&self) -> Result<(), glib::BoolError> {
        let settings = gio::Settings::new(APP_ID);
        let (width, height) = self.main_window.default_size();

        settings.set_int("window-width", width)?;
        settings.set_int("window-height", height)?;

        settings.set_boolean("is-maximized", self.main_window.is_maximized())?;

        Ok(())
    }

    fn load_window_size(&self) {
        let settings = gio::Settings::new(APP_ID);

        let width = settings.int("window-width");
        let height = settings.int("window-height");
        let is_maximized = settings.boolean("is-maximized");

        self.main_window.set_default_size(width, height);

        if is_maximized {
            self.main_window.maximize();
        }
    }
}
