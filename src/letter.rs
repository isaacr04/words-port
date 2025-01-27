use relm4::{
    factory::{positions::GridPosition, Position},
    gtk::{
        self,
        prelude::{ButtonExt, WidgetExt},
    },
    prelude::FactoryComponent,
    FactorySender, RelmWidgetExt,
};

#[derive(Debug, PartialEq, Copy, Clone)]
pub enum Format {
    NotUsed,
    NoMatch,
    Match,
    ExactMatch,
}

impl Format {
    pub fn to_osb_format(&self) -> crate::onscreen_button::Format {
        match self {
            Format::NotUsed => crate::onscreen_button::Format::NoMatch,
            Format::NoMatch => crate::onscreen_button::Format::NoMatch,
            Format::Match => crate::onscreen_button::Format::Match,
            Format::ExactMatch => crate::onscreen_button::Format::ExactMatch,
        }
    }
}

#[derive(Debug, Clone, Copy, Eq, Hash, PartialEq)]
pub struct Coord {
    pub column: usize,
    pub row: usize,
}

#[derive(Debug)]
pub struct Letter {
    pub value: String,
    format: Format,
    selected: bool,
    incorrect: bool,
}

#[derive(Debug)]
pub enum LetterMsgOut {
    Selected(Coord),
}

#[derive(Debug)]
pub enum LetterMsgIn {
    SetContent(Option<String>),
    SetFormat(Format),
    SetSelected(bool),
    SetIncorrect(bool),
}

impl Position<GridPosition, Coord> for Letter {
    fn position(&self, index: &Coord) -> GridPosition {
        GridPosition {
            column: index.column as i32,
            row: index.row as i32,
            width: 1,
            height: 1,
        }
    }
}

#[relm4::factory(pub)]
impl FactoryComponent for Letter {
    type Init = (usize, Format);
    type Index = Coord;
    type Input = LetterMsgIn;
    type Output = LetterMsgOut;
    type CommandOutput = ();
    type ParentWidget = gtk::Grid;

    view! {
        root = &gtk::Button {
            set_margin_all: 1,
            set_hexpand: true,
            set_vexpand: true,
            set_can_focus: false,
            #[watch]
            set_label: &self.value,
            #[watch]
            set_css_classes: { match &self.format {
                Format::NotUsed => &["letter"],
                Format::NoMatch  => &["letter", "no_match"],
                Format::Match  => &["letter", "match"],
                Format::ExactMatch => &["letter", "exact"],
            }},
            connect_clicked[sender, index] => move |_| {
                sender.output(LetterMsgOut::Selected(index)).unwrap();
            },
            #[watch]
            add_css_class: if self.selected == true { "selected" } else {"none"},
            #[watch]
            add_css_class: if self.incorrect == true { "shake" } else {"none"},
        }
    }

    fn init_model(value: Self::Init, index: &Coord, _sender: FactorySender<Self>) -> Self {
        Self {
            format: value.1,
            value: String::new(),
            incorrect: false,
            selected: if index.column == 0 && index.row == 0 {
                true
            } else {
                false
            },
        }
    }

    fn update(&mut self, msg: Self::Input, _sender: FactorySender<Self>) {
        match msg {
            LetterMsgIn::SetContent(v) => {
                self.value = if let Some(v) = v {
                    v.to_string()
                } else {
                    String::new()
                }
            }
            LetterMsgIn::SetFormat(f) => self.format = f,
            LetterMsgIn::SetSelected(v) => self.selected = v,
            LetterMsgIn::SetIncorrect(v) => self.incorrect = v,
        }
    }
}
