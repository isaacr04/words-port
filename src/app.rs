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
    ApplicationExt, ApplicationWindowExt, ButtonExt, GtkWindowExt, OrientableExt, SettingsExt,
    WidgetExt,
};
use gtk::{gio, glib};
use rand::seq::IteratorRandom;
use read_word_list::{read_word_list, WordList};
use relm4::abstractions::Toaster;
use relm4::adw::Toast;
use relm4::factory::FactoryHashMap;
use relm4::gtk::glib::{GString, Propagation};
use relm4::gtk::{Align, EventControllerKey};
use relm4::RelmWidgetExt;
use relm4::{
    actions::{RelmAction, RelmActionGroup},
    adw,
    gtk::{self, prelude::GridExt},
    main_application, Component, ComponentController, ComponentParts, ComponentSender, Controller,
};

static TRIES: usize = 6;

pub(super) struct App {
    letters: FactoryHashMap<Coord, Letter>,
    about_dialog: Controller<AboutDialog>,
    selected_letter: Coord,
    word: String,
    width: usize,
    attempts: usize,
    word_list: WordList,
    keyboard_rows: Vec<FactoryHashMap<Key, OnScreenButton>>,
    current_ui_page: &'static str,
    game_won: bool,
    toaster: Toaster,
    toast_words_in_dictionary_displayed: bool,
    /// used, to check if we backspace should delete the last or the second last letter
    index_of_last_entered_letter: usize,
}

#[derive(Debug)]
pub(super) enum AppMsg {
    SelectField(Coord),
    GameOver(bool),
    StartNewGame,
    EnterLetter(char),
    MoveCursor(isize),
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
        primary_menu: {
            section! {
                "_Keyboard" => ShortcutsAction,
                "_About Words!" => AboutAction,
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
                pack_start = &gtk::Button {
                    set_label: "_New",
                    set_can_focus: false,
                    set_use_underline: true,
                    connect_clicked => AppMsg::StartNewGame,
                },
                pack_end = &gtk::MenuButton {
                    set_icon_name: "open-menu-symbolic",
                    set_menu_model: Some(&primary_menu),
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

        let current_game = "English";
        let number_of_letters = 6;

        let word_list = read_word_list(current_game, number_of_letters).unwrap();

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
            width: 0,
            keyboard_rows: create_empty_on_screen_button_rows(&sender),
            current_ui_page: "game",
            game_won: false,
            toaster: Toaster::default(),
            toast_words_in_dictionary_displayed: false,
            index_of_last_entered_letter: 0,
            word_list,
        };

        root.add_controller(keyboard_events_controller(sender.clone()));

        let letter_grid = model.letters.widget();
        let keyboard_row_1 = model.keyboard_rows[0].widget();
        let keyboard_row_2 = model.keyboard_rows[1].widget();
        let keyboard_row_3 = model.keyboard_rows[2].widget();

        let toast_overlay = model.toaster.overlay_widget();

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
                self.width = self.word.chars().count();
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
                    self.width
                );
                // if on last position, delete letter under cursor, if there is any
                if selected.column == self.width - 1 // are we on the last field
                    && self.index_of_last_entered_letter != self.width - 2 // and we just did not the second last letter
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
                if content_of_current_attempt.chars().count() < self.width {
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

fn line_to_keys(line: &str) -> Vec<Key> {
    let mut keys = vec![];
    for key in line.split(',') {
        match key {
            "SEND" => keys.push(Key::Enter),
            "DEL" => keys.push(Key::Del),
            c => keys.push(Key::Letter(c.chars().next().expect("No Letter found."))),
        }
    }
    keys
}

impl App {
    fn create_empty_field(&mut self) {
        self.letters.clear();
        for column in 0..self.width {
            for row in 0..TRIES {
                self.letters
                    .insert(Coord { column, row }, (self.width, Format::NotUsed));
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
        for column in 0..self.width {
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
mod tests {
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
