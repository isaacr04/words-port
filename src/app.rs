use std::collections::{HashMap, HashSet};
use std::{char, usize};

use crate::letter::{Coord, Format, LetterMsgIn};
use crate::onscreen_button::{self, OnScreenButton, OnScreenButtonMsgIn, OnScreenButtonMsgOut};
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
use relm4::factory::FactoryHashMap;
use relm4::gtk::glib::Propagation;
use relm4::gtk::{Align, EventControllerKey};
use relm4::{
    actions::{RelmAction, RelmActionGroup},
    adw,
    gtk::{self, prelude::GridExt},
    main_application, Component, ComponentController, ComponentParts, ComponentSender, Controller,
    SimpleComponent,
};

static TRIES: usize = 6;
static WORDS_FILE: &str = include_str!("../data/resources/wordlists/words.txt");
static KEYS_FILE: &str = include_str!("../data/resources/wordlists/keys.txt");

pub(super) struct App {
    letters: FactoryHashMap<Coord, Letter>,
    about_dialog: Controller<AboutDialog>,
    selected_letter: Coord,
    word: String,
    width: usize,
    attempts: usize,
    allowed_words: HashSet<&'static str>,
    allowed_letters: HashSet<char>,
    keyboard_rows: Vec<FactoryHashMap<OnScreenButtonMsgOut, OnScreenButton>>,
    current_page: &'static str,
    game_won: bool,
}

#[derive(Debug)]
pub(super) enum AppMsg {
    SelectField(Coord),
    GameOver(bool),
    StartNewGame,
    Letter(char),
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
                "/org/codeberg/petsoi/wordle/gtk/help-overlay.ui"
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
            set_hexpand: true,
            set_vexpand: true,

            adw::HeaderBar {
                pack_start = &gtk::Button {
                    set_label: "New",
                    set_can_focus: false,
                    connect_clicked => AppMsg::StartNewGame,
                },
                pack_end = &gtk::MenuButton {
                    set_icon_name: "open-menu-symbolic",
                    set_menu_model: Some(&primary_menu),
                }
            },

            gtk::Stack {
                #[watch]
                set_visible_child_name: model.current_page,

                add_child=&gtk::Box{
                    set_orientation: gtk::Orientation::Vertical,

                    #[local_ref]
                    letter_grid -> gtk::Grid {
                        set_orientation: gtk::Orientation::Horizontal,
                        set_column_spacing: 0,
                        set_row_spacing: 0,
                        set_halign: Align::Center,
                        //set_hexpand: true,
                        //set_vexpand: true,
                    },
                    gtk::Box {
                        set_orientation: gtk::Orientation::Vertical,
                        set_vexpand: true,
                        set_hexpand: true,

                        #[local_ref]
                        keyboard_row_1 -> gtk::Box {
                            set_orientation: gtk::Orientation::Horizontal,
                            set_hexpand: true,
                            set_vexpand: true,
                            // set_halign: Align::Center,
                        },
                        #[local_ref]
                        keyboard_row_2 -> gtk::Box {
                            set_orientation: gtk::Orientation::Horizontal,
                            set_hexpand: true,
                            set_vexpand: true,
                            // set_halign: Align::Center,
                        },
                        #[local_ref]
                        keyboard_row_3 -> gtk::Box {
                            set_orientation: gtk::Orientation::Horizontal,
                            set_hexpand: true,
                            set_vexpand: true,
                            // set_halign: Align::Center,
                        }
                    }
                } -> { set_name: "game" },

                add_child = &gtk::Box{
                        set_orientation: gtk::Orientation::Vertical,
                        gtk::Label {
                            #[watch]
                            set_label: if model.game_won {"Congratulation!"} else {"Game Over"},
                            set_css_classes: &["title-1"],
                        },
                        gtk::Label {
                            #[watch]
                            set_label: &format!("The word was {}", model.word ),
                        },
                        gtk::Label {
                            #[watch]
                            set_label: &format!("You had {} tries", model.attempts ),
                        }
                    } -> { set_name: "game_over" }
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

        let allowed_words = WORDS_FILE.lines().collect();

        let letters =
            FactoryHashMap::builder()
                .launch_default()
                .forward(sender.input_sender(), |msg| match msg {
                    LetterMsgOut::Selected(index) => AppMsg::SelectField(index),
                });

        let model = Self {
            about_dialog,
            letters,
            selected_letter: Coord { column: 0, row: 0 },
            word: String::new(),
            attempts: 0,
            allowed_words,
            width: 0,
            allowed_letters: HashSet::new(),
            keyboard_rows: create_empty_on_screen_button_rows(&sender),
            current_page: "game",
            game_won: false,
        };

        root.add_controller(keyboard_events_controller(sender.clone()));

        let letter_grid = model.letters.widget();
        let keyboard_row_1 = model.keyboard_rows[0].widget();
        let keyboard_row_2 = model.keyboard_rows[1].widget();
        let keyboard_row_3 = model.keyboard_rows[2].widget();

        let widgets = view_output!();

        register_actions(sender.clone(), &widgets, &model);

        widgets.load_window_size();

        sender.input(AppMsg::StartNewGame);

        ComponentParts { model, widgets }
    }

    fn update(&mut self, message: Self::Input, sender: ComponentSender<Self>) {
        let selected = self.selected_letter;

        match message {
            AppMsg::Quit => main_application().quit(),
            AppMsg::SelectField(index) => self.select_field(index),
            AppMsg::StartNewGame => {
                self.word = pick_random_word(&self.allowed_words);
                println!("New Word: {}", self.word);
                self.width = self.word.chars().count();
                self.attempts = 0;
                self.selected_letter = Coord { column: 0, row: 0 };
                self.current_page = "game";
                self.create_empty_field();
                self.create_new_keyboard_and_set_allowed_letters(KEYS_FILE);
            }
            AppMsg::Letter(c) => {
                let upper_case = c.to_uppercase().to_string(); // TODO: Logic needs to be improved if we want to support e.g. ß => SS
                if upper_case.chars().count() == 1
                    && self
                        .allowed_letters
                        .contains(&upper_case.chars().next().unwrap())
                {
                    self.letters.send(
                        &selected,
                        LetterMsgIn::SetContent(Some(c.to_uppercase().to_string())),
                    );
                    self.move_selection_by(1);
                }
            }
            AppMsg::Delete => self.letters.send(&selected, LetterMsgIn::SetContent(None)),
            AppMsg::Backspace => {
                // if on last postion, delete letter under cursor, if there is any
                if selected.column == self.width - 1
                    && !self.letters.get(&selected).unwrap().value.is_empty()
                {
                    sender.input(AppMsg::Delete);
                    return;
                }
                self.move_selection_by(-1);
                self.letters
                    .send(&self.selected_letter, LetterMsgIn::SetContent(None))
            }
            AppMsg::Enter => {
                let Some(content_of_current_attempt) = self.get_entered_word() else {
                    return;
                };
                if content_of_current_attempt == self.word {
                    self.attempts += 1;
                    sender.input(AppMsg::GameOver(true));
                    return;
                }
                if content_of_current_attempt.chars().count() < self.width {
                    return;
                }

                if !self
                    .allowed_words
                    .contains(content_of_current_attempt.as_str())
                {
                    return;
                }

                self.set_color_of_letters_according_matching(content_of_current_attempt);

                self.attempts += 1;

                if self.attempts >= TRIES {
                    sender.input(AppMsg::GameOver(false));
                    return;
                }

                self.make_attempt_row_selectable();

                sender.input(AppMsg::SelectField(Coord {
                    column: 0,
                    row: self.attempts,
                }));
            }
            AppMsg::GameOver(won) => {
                self.game_won = won;
                self.current_page = "game_over"
            }
        }
    }

    fn shutdown(&mut self, widgets: &mut Self::Widgets, _output: relm4::Sender<Self::Output>) {
        widgets.save_window_size().unwrap();
    }
}

fn create_empty_on_screen_button_rows(
    sender: &ComponentSender<App>,
) -> Vec<FactoryHashMap<OnScreenButtonMsgOut, OnScreenButton>> {
    (0..3)
        .into_iter()
        .map(|_| {
            FactoryHashMap::builder()
                .launch_default()
                .forward(sender.input_sender(), |msg| match msg {
                    OnScreenButtonMsgOut::Letter(c) => AppMsg::Letter(c),
                    OnScreenButtonMsgOut::Enter => AppMsg::Enter,
                    OnScreenButtonMsgOut::Del => AppMsg::Backspace,
                })
        })
        .collect()
}

fn line_to_keys(line: &str) -> Vec<OnScreenButtonMsgOut> {
    let mut keys = vec![];
    for key in line.split(',') {
        match key {
            "SEND" => keys.push(OnScreenButtonMsgOut::Enter),
            "DEL" => keys.push(OnScreenButtonMsgOut::Del),
            c => keys.push(OnScreenButtonMsgOut::Letter(
                c.chars().next().expect("No Letter found."),
            )),
        }
    }
    keys
}

impl App {
    fn create_empty_field(&mut self) {
        self.letters.clear();
        for column in 0..self.width {
            for row in 0..TRIES {
                if row == 0 {
                    self.letters
                        .insert(Coord { column, row }, (self.width, Format::Editable));
                } else {
                    self.letters
                        .insert(Coord { column, row }, (self.width, Format::NotUsed));
                }
            }
        }
    }

    fn select_field(&mut self, coord: Coord) {
        self.letters
            .send(&self.selected_letter, LetterMsgIn::SetSelected(false));

        self.letters.send(&coord, LetterMsgIn::SetSelected(true));
        self.selected_letter = coord;
    }

    fn move_selection_by(&mut self, step: isize) {
        let new_column: isize = self.selected_letter.column as isize + step;

        if new_column >= 0 && new_column < self.width as isize {
            let new_selected_letter = Coord {
                column: new_column as usize,
                row: self.selected_letter.row,
            };
            self.select_field(new_selected_letter);
        }
    }

    fn get_entered_word(&self) -> Option<String> {
        let mut content_of_current_attempt = String::new();
        for column in 0..self.width {
            let c_u = self
                .letters
                .get(&Coord {
                    column,
                    row: self.attempts,
                })?
                .value
                .clone();
            content_of_current_attempt.push_str(&c_u);
        }
        Some(content_of_current_attempt)
    }

    fn set_color_of_letters_according_matching(&mut self, entered_word: String) {
        let mut left_letters = HashMap::new();
        let mut correct_letters_positions = HashSet::new();

        let matches: Vec<_> = self
            .word
            .chars()
            .zip(entered_word.chars())
            .enumerate()
            .collect();

        for (column, (target_char, user_char)) in &matches {
            if target_char == user_char {
                self.letters.send(
                    &Coord {
                        column: *column,
                        row: self.attempts,
                    },
                    LetterMsgIn::SetFormat(Format::ExactMatch),
                );
                self.send_on_screen_button_format(user_char, onscreen_button::Format::ExactMatch);
                correct_letters_positions.insert(column);
            } else {
                left_letters
                    .entry(target_char.to_string())
                    .and_modify(|c| *c += 1)
                    .or_insert(1);
            };
        }

        for (column, (_, user_char)) in &matches {
            if correct_letters_positions.contains(&column) {
                continue;
            }

            if let Some(number) = left_letters.get_mut(&user_char.to_string()) {
                if *number > 0 {
                    self.letters.send(
                        &Coord {
                            column: *column,
                            row: self.attempts,
                        },
                        LetterMsgIn::SetFormat(Format::Match),
                    );
                    self.send_on_screen_button_format(user_char, onscreen_button::Format::Match);

                    *number -= 1;
                    continue;
                }
            }
            self.letters.send(
                &Coord {
                    column: *column,
                    row: self.attempts,
                },
                LetterMsgIn::SetFormat(Format::NoMatch),
            );
            self.send_on_screen_button_format(user_char, onscreen_button::Format::NoMatch);
        }
    }

    fn send_on_screen_button_format(&mut self, user_char: &char, format: onscreen_button::Format) {
        for row in &self.keyboard_rows {
            if row.get(&OnScreenButtonMsgOut::Letter(*user_char)).is_some() {
                row.send(
                    &OnScreenButtonMsgOut::Letter(*user_char),
                    OnScreenButtonMsgIn::SetFormat(format),
                );
            }
        }
    }

    fn create_new_keyboard_and_set_allowed_letters(&mut self, lines: &str) {
        self.keyboard_rows.iter_mut().for_each(|row| {
            row.clear();
        });

        self.allowed_letters.clear();

        for (row, line) in self.keyboard_rows.iter_mut().zip(lines.lines()) {
            for b in line_to_keys(line) {
                if let OnScreenButtonMsgOut::Letter(c) = b {
                    self.allowed_letters.insert(c);
                }
                row.insert(b, b);
            }
        }
    }

    fn make_attempt_row_selectable(&mut self) {
        for i in 0..self.width {
            self.letters.send(
                &Coord {
                    column: i,
                    row: self.attempts,
                },
                LetterMsgIn::SetFormat(Format::Editable),
            )
        }
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

    controller.connect_key_pressed(move |_, keyval, _, _| {
        if let Some(c) = keyval.to_unicode() {
            match c {
                '\u{8}' => sender.input(AppMsg::Backspace),
                '\u{7f}' => sender.input(AppMsg::Delete),
                c => sender.input(AppMsg::Letter(c)),
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
