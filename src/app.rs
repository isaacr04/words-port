mod read_word_list;

use std::collections::{HashMap, HashSet};
use std::time::Duration;
use std::{char, usize};

use crate::letter::{Coord, Format, LetterMsgIn};
use crate::onscreen_button::{self, Key, OnScreenButton, OnScreenButtonMsgIn};
use crate::{
    config::{APP_ID, PROFILE},
    letter::LetterMsgOut,
};
use crate::{letter::Letter, modals::about::AboutDialog};
use gtk::prelude::{
    ApplicationExt, ApplicationWindowExt, ButtonExt, GtkWindowExt, OrientableExt, PopoverExt,
    SettingsExt, WidgetExt,
};
use gtk::{gio, glib};
use rand::seq::IteratorRandom;
use read_word_list::{get_available_word_lists, read_word_list, WordList};
use relm4::abstractions::Toaster;
use relm4::adw::Toast;
use relm4::factory::FactoryHashMap;
use relm4::gtk::glib::object::Cast;
use relm4::gtk::glib::{GString, Propagation};
use relm4::gtk::prelude::ToggleButtonExt;
use relm4::gtk::{Align, EventControllerKey};
use relm4::RelmWidgetExt;
use relm4::{
    actions::{RelmAction, RelmActionGroup},
    adw,
    gtk::{self, prelude::GridExt},
    main_application, Component, ComponentController, ComponentParts, ComponentSender, Controller,
};

static TRIES: usize = 6;
static DEFAULT_GAME_NAME: &str = "English";

pub(super) struct App {
    letters: FactoryHashMap<Coord, Letter>,
    about_dialog: Controller<AboutDialog>,
    selected_letter: Coord,
    word: String,
    number_of_letters: usize,
    attempts: usize,
    word_list: WordList,
    keyboard_rows: Vec<FactoryHashMap<Key, OnScreenButton>>,
    current_ui_page: &'static str,
    game_won: bool,
    toaster: Toaster,
    toast_words_in_dictionary_displayed: bool,
    /// used, to check if we backspace should delete the last or the second last letter
    index_of_last_entered_letter: usize,
    available_word_lists: Vec<String>,
    index_of_currently_selected_word_list: usize,
    current_word_list_name: String,
}

#[derive(Debug)]
pub(super) enum AppMsg {
    SelectField(Coord),
    GameOver(bool),
    StartNewGame,
    EnterLetter(char),
    MoveCursor(isize),
    SetLengthTo(usize),
    SwitchToWordList(String),
    EnterWord,
    Delete,
    Backspace,
    Space,
    Quit,
}

#[derive(Debug)]
pub(super) enum CommandMsg {
    ResetIncorrectWord,
}

relm4::new_action_group!(pub(super) WindowActionGroup, "win");
relm4::new_stateless_action!(pub(super) ShortcutsAction, WindowActionGroup, "show-help-overlay");
relm4::new_stateless_action!(AboutAction, WindowActionGroup, "about");

#[relm4::component(pub)]
impl Component for App {
    type Init = ();
    type Input = AppMsg;
    type Output = ();
    type Widgets = AppWidgets;
    type CommandOutput = CommandMsg;

    menu! {
        main_menu: {
            section! {
                "_Keyboard" => ShortcutsAction,
                "_About Words!" => AboutAction,
            }
        }
    }

    view! {
    #[root]
    main_window = adw::ApplicationWindow::new(&main_application()) {
        set_visible: true,

        connect_close_request[sender] => move |_| {
            sender.input(AppMsg::Quit);
            glib::Propagation::Stop
        },

        #[wrap(Some)]
        set_help_overlay: shortcuts = &gtk::Builder::from_resource(
                "/page/codeberg/petsoi/words/gtk/help-overlay.ui"
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
                #[name = "new_button"]
                pack_start = &adw::SplitButton {
                    set_label: "New",
                    set_hexpand: true,
                    connect_clicked => AppMsg::StartNewGame,
                    set_popover: Some(&game_menu),
                    set_can_focus: false,
                },
                pack_end = &gtk::MenuButton {
                    set_icon_name: "open-menu-symbolic",
                    set_menu_model: Some(&main_menu),
                    set_direction: gtk::ArrowType::Down,
                    set_can_focus: false,
                }
            },
            gtk::Stack {
                #[watch]
                set_visible_child_name: model.current_ui_page,

                add_child=&gtk::Box{
                    set_orientation: gtk::Orientation::Vertical,
                    set_vexpand: true,
                    set_hexpand: true,

                    #[local_ref]
                    toast_overlay -> adw::ToastOverlay {
                        #[local_ref]
                        letter_grid -> gtk::Grid {
                            set_orientation: gtk::Orientation::Horizontal,
                            set_column_homogeneous: true,
                            set_row_homogeneous: true,
                            set_column_spacing: 0,
                            set_row_spacing: 0,
                            // set_halign: Align::Center,
                            set_hexpand: true,
                            //set_vexpand: true,
                            set_row_spacing: 1,
                            set_column_spacing: 1,
                        }
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
                        set_halign: Align::Center,
                        set_valign: Align::Center,
                        gtk::Label {
                            #[watch]
                            set_label: if model.game_won {"Congratulations!"} else {"Game Over"},
                            set_css_classes: &["title-1"],
                        },
                        gtk::Label {
                            #[watch]
                            set_label: &format!("The word we were looking for was {}.", model.word ),
                            set_css_classes: &["title-3"],
                            set_margin_all: 20,
                            set_wrap: true,
                            set_justify: gtk::Justification::Center,
                        },
                        gtk::Label {
                            #[watch]
                            set_label: &if model.game_won {
                                    if model.attempts == 1 {
                                        "You only needed one attempt!!".to_owned()
                                    } else {
                                        format!("You needed {} attempts.", model.attempts)
                                    }
                                } else {
                                    "Good luck next time!".to_owned()
                                },
                            set_css_classes: &["title-3"],
                            set_wrap: true,
                            set_justify: gtk::Justification::Center,
                        }
                    } -> { set_name: "game_over" }
                }
            }
        },

        #[local_ref]
        game_menu -> gtk::Popover {
            set_parent: &new_button,
            set_autohide: true,
            gtk::Box {
                set_orientation: gtk::Orientation::Vertical,
                set_margin_all: 10,

                gtk::Label {
                    set_label: "Language:",
                    set_align: Align::Start,
                },

                #[name = "dropdown"]
                gtk::DropDown::from_strings(model.available_word_lists.iter().map(|s| s.as_str()).collect::<Vec<&str>>().iter().as_slice()) {
                    set_selected: model.index_of_currently_selected_word_list as u32,
                    connect_selected_notify[sender] => move |dropdown| {
                        if let Some(selected_item) = dropdown.selected_item() {
                            if let Ok(string_object) = selected_item.downcast::<gtk::StringObject>() {
                                let new_list = string_object.string();
                                sender.input(AppMsg::SwitchToWordList(new_list.to_string()));
                            }
                        }
                    }
                },

                gtk::Label {
                    set_label: "Letters:",
                    set_align: Align::Start,
                },

                gtk::Box {
                    set_orientation: gtk::Orientation::Horizontal,

                    #[name = "toggle_button_4"]
                    gtk::ToggleButton {
                        set_label: "4",
                        set_css_classes: &["numb_letter"],
                        #[watch]
                        set_sensitive: model.word_list.available_word_lengths.l4,
                        #[watch]
                        set_active: model.number_of_letters == 4,
                        connect_toggled[sender] => move |btn| {
                            if btn.is_active() {
                                sender.input(AppMsg::SetLengthTo(4));
                            }
                        }
                    },
                    #[name = "toggle_button_5"]
                    gtk::ToggleButton {
                        set_label: "5",
                        set_css_classes: &["numb_letter"],
                        set_group: Some(&toggle_button_4),
                        #[watch]
                        set_sensitive: model.word_list.available_word_lengths.l5,
                        #[watch]
                        set_active: model.number_of_letters == 5,
                        connect_toggled[sender] => move |btn| {
                            if btn.is_active() {
                                sender.input(AppMsg::SetLengthTo(5));
                            }
                        }
                    },
                    #[name = "toggle_button_6"]
                    gtk::ToggleButton {
                        set_label: "6",
                        set_css_classes: &["numb_letter"],
                        set_group: Some(&toggle_button_4),
                        #[watch]
                        set_sensitive: model.word_list.available_word_lengths.l6,
                        #[watch]
                        set_active: model.number_of_letters == 6,
                        connect_toggled[sender] => move |btn| {
                            if btn.is_active() {
                                sender.input(AppMsg::SetLengthTo(6));
                            }
                        }                    },
                    #[name = "toggle_button_7"]
                    gtk::ToggleButton {
                        set_label: "7",
                        set_css_classes: &["numb_letter"],
                        set_group: Some(&toggle_button_4),
                        #[watch]
                        set_sensitive: model.word_list.available_word_lengths.l7,
                        #[watch]
                        set_active: model.number_of_letters == 7,
                        connect_toggled[sender] => move |btn| {
                            if btn.is_active() {
                                sender.input(AppMsg::SetLengthTo(7));
                            }
                        }                    },
                    #[name = "toggle_button_8"]
                    gtk::ToggleButton {
                        set_label: "8",
                        set_css_classes: &["numb_letter"],
                        set_group: Some(&toggle_button_4),
                        #[watch]
                        set_sensitive: model.word_list.available_word_lengths.l8,
                        #[watch]
                        set_active: model.number_of_letters == 8,
                        connect_toggled[sender] => move |btn| {
                            if btn.is_active() {
                                sender.input(AppMsg::SetLengthTo(8));
                            }
                        }                    },
                    #[name = "toggle_button_9"]
                    gtk::ToggleButton {
                        set_label: "9",
                        set_css_classes: &["numb_letter"],
                        set_group: Some(&toggle_button_4),
                        #[watch]
                        set_sensitive: model.word_list.available_word_lengths.l9,
                        #[watch]
                        set_active: model.number_of_letters == 9,
                        connect_toggled[sender] => move |btn| {
                            if btn.is_active() {
                                sender.input(AppMsg::SetLengthTo(9));
                            }
                        }                    },
                    #[name = "toggle_button_10"]
                    gtk::ToggleButton {
                        set_label: "10",
                        set_css_classes: &["numb_letter"],
                        set_group: Some(&toggle_button_4),
                        #[watch]
                        set_sensitive: model.word_list.available_word_lengths.l10,
                        #[watch]
                        set_active: model.number_of_letters == 10,
                        connect_toggled[sender] => move |btn| {
                            if btn.is_active() {
                                sender.input(AppMsg::SetLengthTo(10));
                            }
                        }                    },
                    #[name = "toggle_button_11"]
                    gtk::ToggleButton {
                        set_label: "11",
                        set_css_classes: &["numb_letter"],
                        set_group: Some(&toggle_button_4),
                        #[watch]
                        set_sensitive: model.word_list.available_word_lengths.l11,
                        #[watch]
                        set_active: model.number_of_letters == 11,
                        connect_toggled[sender] => move |btn| {
                            if btn.is_active() {
                                sender.input(AppMsg::SetLengthTo(11));
                            }
                        }
                    },
                }
            },
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

        let settings = gio::Settings::new(APP_ID);

        let last_game = settings.string("game-name").to_string();
        let last_number_of_letters = settings.uint("number-of-letters") as usize;

        let available_word_lists = get_available_word_lists().unwrap();

        let fallback = available_word_lists[0].to_owned();

        let (index_of_currently_selected_word_list, list_name) =
            if let Some(i) = get_index(&available_word_lists, &last_game) {
                (i, last_game)
            } else {
                if let Some(i) = get_index(&available_word_lists, &DEFAULT_GAME_NAME) {
                    (i, "English".to_owned())
                } else {
                    (0, fallback)
                }
            };

        let word_list = read_word_list(&list_name, last_number_of_letters)
            .unwrap()
            .unwrap();

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
            number_of_letters: last_number_of_letters,
            keyboard_rows: create_empty_on_screen_button_rows(&sender),
            current_ui_page: "game",
            game_won: false,
            toaster: Toaster::default(),
            toast_words_in_dictionary_displayed: false,
            index_of_last_entered_letter: 0,
            word_list,
            available_word_lists,
            index_of_currently_selected_word_list,
            current_word_list_name: list_name.to_owned(),
        };

        root.add_controller(keyboard_events_controller(sender.clone()));

        let letter_grid = model.letters.widget();
        let keyboard_row_1 = model.keyboard_rows[0].widget();
        let keyboard_row_2 = model.keyboard_rows[1].widget();
        let keyboard_row_3 = model.keyboard_rows[2].widget();

        let toast_overlay = model.toaster.overlay_widget();

        let game_menu = gtk::Popover::new();

        let widgets = view_output!();

        register_actions(sender.clone(), &widgets, &model);

        widgets.load_window_size();

        sender.input(AppMsg::StartNewGame);

        ComponentParts { model, widgets }
    }

    fn update(&mut self, message: Self::Input, sender: ComponentSender<Self>, _: &Self::Root) {
        let selected = self.selected_letter;

        match message {
            AppMsg::Quit => main_application().quit(),
            AppMsg::SelectField(index) => {
                self.index_of_last_entered_letter = 0;
                if index.row == self.attempts {
                    self.select_field(index)
                }
            }
            AppMsg::MoveCursor(step) => {
                self.index_of_last_entered_letter = 0;
                self.move_selection_by(step)
            }
            AppMsg::StartNewGame => {
                self.word = pick_random_word(&self.word_list.allowed_words);
                println!("New Word: {}", self.word);
                self.attempts = 0;
                self.index_of_last_entered_letter = 0;
                self.selected_letter = Coord { column: 0, row: 0 };
                self.current_ui_page = "game";
                self.create_empty_field();
                self.create_new_keyboard();
            }
            AppMsg::EnterLetter(c) => {
                let upper_case = c.to_uppercase().to_string(); // TODO: Logic needs to be improved if we want to support e.g. ß => SS
                if upper_case.chars().count() == 1
                    && self
                        .word_list
                        .allowed_letters
                        .contains(&upper_case.chars().next().unwrap())
                {
                    self.letters.send(
                        &selected,
                        LetterMsgIn::SetContent(Some(c.to_uppercase().to_string())),
                    );
                    self.index_of_last_entered_letter = self.selected_letter.column;
                    self.move_selection_by(1);
                }
            }
            AppMsg::Delete => self.letters.send(&selected, LetterMsgIn::SetContent(None)),
            AppMsg::Space => {
                self.letters.send(&selected, LetterMsgIn::SetContent(None));
                self.move_selection_by(1);
            }
            AppMsg::Backspace => {
                dbg!(
                    selected.column,
                    self.index_of_last_entered_letter,
                    self.number_of_letters
                );
                // if on last position, delete letter under cursor, if there is any
                if selected.column == self.number_of_letters - 1 // are we on the last field
                    && self.index_of_last_entered_letter != self.number_of_letters - 2 // and we just did not the second last letter
                    && !self.letters.get(&selected).unwrap().value.is_empty()
                // and the last letter is not empty
                {
                    sender.input(AppMsg::Delete);
                    return;
                }
                self.move_selection_by(-1);
                self.letters
                    .send(&self.selected_letter, LetterMsgIn::SetContent(None))
            }
            AppMsg::EnterWord => {
                let Some(content_of_current_attempt) = self.get_entered_word() else {
                    return;
                };
                if content_of_current_attempt == self.word {
                    self.attempts += 1;
                    sender.input(AppMsg::GameOver(true));
                    return;
                }
                if content_of_current_attempt.chars().count() < self.number_of_letters {
                    return;
                }

                if !self
                    .word_list
                    .allowed_words
                    .contains(content_of_current_attempt.as_str())
                {
                    if !self.toast_words_in_dictionary_displayed {
                        let toast = Toast::new("Words have to be in the dictionary.");
                        self.toaster.add_toast(toast);
                        self.toast_words_in_dictionary_displayed = true;
                    }

                    self.set_word_to_incorrect(true);
                    sender.spawn_oneshot_command(|| {
                        std::thread::sleep(Duration::from_millis(500));
                        CommandMsg::ResetIncorrectWord
                    });
                    return;
                }

                self.set_color_of_letters_according_matching(&content_of_current_attempt);

                self.attempts += 1;

                if self.attempts >= TRIES {
                    sender.input(AppMsg::GameOver(false));
                    return;
                }

                sender.input(AppMsg::SelectField(Coord {
                    column: 0,
                    row: self.attempts,
                }));
            }

            AppMsg::GameOver(won) => {
                self.game_won = won;
                self.current_ui_page = "game_over"
            }

            AppMsg::SetLengthTo(length) => {
                if length != self.number_of_letters {
                    println!(
                        "Current wordlist {}, set length to {length}",
                        &self.current_word_list_name,
                    );
                    self.word_list = read_word_list(&self.current_word_list_name, length)
                        .unwrap()
                        .unwrap();

                    self.number_of_letters = length;

                    sender.input(AppMsg::StartNewGame);
                }
            }
            AppMsg::SwitchToWordList(name) => {
                self.word_list = if let Some(word_list) =
                    read_word_list(&name, self.number_of_letters).unwrap()
                {
                    word_list
                } else {
                    self.number_of_letters = 5;
                    read_word_list(&name, self.number_of_letters)
                        .unwrap()
                        .unwrap()
                };
                self.current_word_list_name = name;
                sender.input(AppMsg::StartNewGame);
            }
        }
    }

    fn update_cmd(
        &mut self,
        message: Self::CommandOutput,
        _sender: ComponentSender<Self>,
        _: &Self::Root,
    ) {
        match message {
            CommandMsg::ResetIncorrectWord => self.set_word_to_incorrect(false),
        }
    }

    fn shutdown(&mut self, widgets: &mut Self::Widgets, _output: relm4::Sender<Self::Output>) {
        widgets.save_window_size().unwrap();

        let settings = gio::Settings::new(APP_ID);

        settings
            .set_string("game-name", &self.current_word_list_name)
            .expect("Failed to save last-game");
        settings
            .set_uint("number-of-letters", self.number_of_letters as u32)
            .expect("Failed to save number-of-letters");
    }
}

fn create_empty_on_screen_button_rows(
    sender: &ComponentSender<App>,
) -> Vec<FactoryHashMap<Key, OnScreenButton>> {
    (0..3)
        .into_iter()
        .map(|_| {
            FactoryHashMap::builder()
                .launch_default()
                .forward(sender.input_sender(), |msg| match msg {
                    Key::Letter(c) => AppMsg::EnterLetter(c),
                    Key::Enter => AppMsg::EnterWord,
                    Key::Del => AppMsg::Backspace,
                })
        })
        .collect()
}

impl App {
    fn create_empty_field(&mut self) {
        self.letters.clear();
        for column in 0..self.number_of_letters {
            for row in 0..TRIES {
                self.letters.insert(
                    Coord { column, row },
                    (self.number_of_letters, Format::NotUsed),
                );
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

        if new_column >= 0 && new_column < self.number_of_letters as isize {
            let new_selected_letter = Coord {
                column: new_column as usize,
                row: self.selected_letter.row,
            };
            self.select_field(new_selected_letter);
        }
    }

    fn get_entered_word(&self) -> Option<String> {
        let mut content_of_current_attempt = String::new();
        for column in 0..self.number_of_letters {
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

    fn set_color_of_letters_according_matching(&mut self, entered_word: &str) {
        let format = calculate_color(&self.word, entered_word);

        for (column, (format, c)) in format.into_iter().zip(entered_word.chars()).enumerate() {
            self.letters.send(
                &Coord {
                    column,
                    row: self.attempts,
                },
                LetterMsgIn::SetFormat(format),
            );

            self.send_on_screen_button_format(&c, format.to_osb_format());
        }
    }

    fn send_on_screen_button_format(&mut self, user_char: &char, format: onscreen_button::Format) {
        for row in &self.keyboard_rows {
            if row.get(&Key::Letter(*user_char)).is_some() {
                row.send(
                    &Key::Letter(*user_char),
                    OnScreenButtonMsgIn::SetFormat(format),
                );
                return;
            }
        }
    }

    fn create_new_keyboard(&mut self) {
        self.keyboard_rows.iter_mut().for_each(|row| {
            row.clear();
        });

        for (row, key_row) in self.keyboard_rows.iter_mut().zip(&self.word_list.keys) {
            for b in key_row {
                row.insert(*b, *b);
            }
        }
    }

    fn set_word_to_incorrect(&mut self, v: bool) {
        for column in 0..self.number_of_letters {
            self.letters.send(
                &Coord {
                    column,
                    row: self.attempts,
                },
                LetterMsgIn::SetIncorrect(v),
            );
        }
    }
}

fn pick_random_word(words: &HashSet<String>) -> String {
    words.iter().choose(&mut rand::rng()).unwrap().clone()
}

fn keyboard_events_controller(sender: ComponentSender<App>) -> EventControllerKey {
    let controller = EventControllerKey::new();
    let right = GString::from("Right");
    let left = GString::from("Left");

    controller.connect_key_pressed(move |_, keyval, _, _| {
        if let Some(name) = keyval.name() {
            if name == right {
                sender.input(AppMsg::MoveCursor(1))
            };
            if name == left {
                sender.input(AppMsg::MoveCursor(-1))
            };
        };
        if let Some(c) = keyval.to_unicode() {
            match c {
                ' ' => sender.input(AppMsg::Space),
                '\u{8}' => sender.input(AppMsg::Backspace),
                '\u{7f}' => sender.input(AppMsg::Delete),
                '\r' => sender.input(AppMsg::EnterWord),
                c => sender.input(AppMsg::EnterLetter(c)),
            }
            Propagation::Stop
        } else {
            Propagation::Proceed
        }
    });
    controller
}

fn register_actions(_sender: ComponentSender<App>, widgets: &AppWidgets, model: &App) {
    let mut actions = RelmActionGroup::<WindowActionGroup>::new();

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

fn get_index(vec: &Vec<String>, s: &str) -> Option<usize> {
    Some(vec.iter().position(|r| r == s)?)
}

fn calculate_color(correct_word: &str, entered_word: &str) -> Vec<Format> {
    let mut format = vec![Format::NoMatch; correct_word.len()];

    let mut left_letters = HashMap::new();
    let mut exact_positions = HashSet::new();

    // First pass: find exact matches and count leftover characters
    for (i, (target_char, user_char)) in correct_word.chars().zip(entered_word.chars()).enumerate()
    {
        if target_char == user_char {
            format[i] = Format::ExactMatch;
            exact_positions.insert(i);
        } else {
            *left_letters.entry(target_char).or_insert(0) += 1;
        }
    }

    // Second pass: find partial matches (ignoring exact matches)
    for (i, user_char) in entered_word.chars().enumerate() {
        if format[i] == Format::ExactMatch {
            continue;
        }

        if let Some(count) = left_letters.get_mut(&user_char) {
            if *count > 0 {
                format[i] = Format::Match;
                *count -= 1;
            }
        }
    }

    format
}

#[cfg(test)]
mod string_finding {
    use super::*;

    #[test]
    fn find_string() {
        let strings = vec!["one".to_owned(), "two".to_owned(), "three".to_owned()];
        let string = "two";

        let actual = get_index(&strings, string);
        assert_eq!(actual, Some(1));
    }

    #[test]
    fn not_find_string() {
        let strings = vec!["one".to_owned(), "two".to_owned(), "three".to_owned()];
        let string = "four";

        let actual = get_index(&strings, string);
        assert_eq!(actual, None);
    }
}

#[cfg(test)]
mod matching {
    use super::*;

    #[test]
    fn test_exact_match() {
        let result = calculate_color("hello", "hello");
        assert_eq!(
            result,
            vec![
                Format::ExactMatch,
                Format::ExactMatch,
                Format::ExactMatch,
                Format::ExactMatch,
                Format::ExactMatch
            ]
        );
    }

    #[test]
    fn test_no_match() {
        let result = calculate_color("hello", "rqdas");
        assert_eq!(
            result,
            vec![
                Format::NoMatch,
                Format::NoMatch,
                Format::NoMatch,
                Format::NoMatch,
                Format::NoMatch
            ]
        );
    }

    #[test]
    fn test_partial_match() {
        let result = calculate_color("hello", "ollzx");
        assert_eq!(
            result,
            vec![
                Format::Match,
                Format::Match,
                Format::ExactMatch,
                Format::NoMatch,
                Format::NoMatch
            ]
        );
    }

    #[test]
    fn test_mixed_match() {
        let result = calculate_color("crate", "trace");
        assert_eq!(
            result,
            vec![
                Format::Match,
                Format::ExactMatch,
                Format::ExactMatch,
                Format::Match,
                Format::ExactMatch
            ]
        );
    }

    #[test]
    fn test_extra_letters() {
        let result = calculate_color("apple", "allee");
        assert_eq!(
            result,
            vec![
                Format::ExactMatch,
                Format::Match,
                Format::NoMatch,
                Format::NoMatch,
                Format::ExactMatch
            ]
        );
    }
}
