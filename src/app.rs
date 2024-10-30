use std::collections::{HashMap, HashSet};
use std::iter::Map;
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
use relm4::actions::AccelsPlus;
use relm4::gtk::glib::Propagation;
use relm4::gtk::EventControllerKey;
use relm4::{
    actions::{RelmAction, RelmActionGroup},
    adw,
    factory::FactoryVecDeque,
    gtk::{self, prelude::GridExt},
    main_application,
    prelude::DynamicIndex,
    Component, ComponentController, ComponentParts, ComponentSender, Controller, SimpleComponent,
};

static TRIES: usize = 6;

pub(super) struct App {
    letters: FactoryVecDeque<Letter>,
    about_dialog: Controller<AboutDialog>,
    selected_letter: usize,
    word: String,
    attempts: usize,
}

#[derive(Debug)]
pub(super) enum AppMsg {
    SelectField(DynamicIndex),
    StartNewGame(String),
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
                        connect_clicked => AppMsg::StartNewGame("COLOR".to_owned()),
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
                    LetterMsgOut::Selected(index) => AppMsg::SelectField(index),
                });

        let model = Self {
            about_dialog,
            letters,
            selected_letter: 0,
            word: String::new(),
            attempts: 0,
        };

        let controller = EventControllerKey::new();

        let s = sender.clone();

        // Connect to the key-pressed signal to handle key presses
        controller.connect_key_pressed(move |_, keyval, _, _| {
            if let Some(c) = keyval.to_unicode() {
                match c {
                    '\u{8}' => s.input(AppMsg::Backspace),
                    '\u{7f}' => s.input(AppMsg::Delete),
                    c => s.input(AppMsg::EnterLetter(c)),
                }
                Propagation::Stop
            } else {
                Propagation::Proceed
            }
        });
        root.add_controller(controller);

        let letter_grid = model.letters.widget();
        let widgets = view_output!();

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

        widgets.load_window_size();

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
            AppMsg::SelectField(index) => select_field(index.current_index()),
            AppMsg::StartNewGame(word) => {
                let width = word.chars().count();
                self.word = word;
                self.attempts = 0;

                letters_guard.clear();
                for i in 0..width * TRIES {
                    if i < width {
                        letters_guard.push_back((width, Format::Editable));
                    } else {
                        letters_guard.push_back((width, Format::NoMatch));
                    }
                }
                self.selected_letter = 0;
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
                let mut left_letters = HashMap::new();
                let mut correct_letters = HashSet::new();
                let width = self.word.chars().count();
                for (i, c) in self.word.chars().enumerate() {
                    let c_u = &letters_guard.get(self.attempts * width + i).unwrap().value;
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

                println!("{left_letters:?} - {correct_letters:?}");

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
                        letters_guard.send(
                            self.attempts * width + i,
                            LetterMsgIn::SetFormat(Format::NoMatch),
                        )
                    }
                }

                self.attempts += 1;

                for i in 0..width {
                    letters_guard.send(
                        self.attempts * width + i,
                        LetterMsgIn::SetFormat(Format::Editable),
                    )
                }
                self.selected_letter = self.attempts * width;
            }
        }
    }

    fn shutdown(&mut self, widgets: &mut Self::Widgets, _output: relm4::Sender<Self::Output>) {
        widgets.save_window_size().unwrap();
    }
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
