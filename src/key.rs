use relm4::{
    factory::{positions::GridPosition, Position},
    gtk::{self, prelude::ButtonExt, prelude::WidgetExt},
    prelude::FactoryComponent,
    FactorySender, RelmWidgetExt,
};

#[derive(Debug, PartialEq)]
pub enum Format {
    NotUsed,
    NoMatch,
    Match,
    ExactMatch,
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
        root = gtk::Button {
            set_margin_all: 1,
            set_has_frame: true,
            #[watch]
            set_label: &self.value,
            #[watch]
            set_css_classes: { match &self.format {
                Format::NotUsed | Format::Editable => &["title-1"],
                Format::NoMatch  => &["title-1", "no_match"],
                Format::Match  => &["title-1", "match"],
                Format::ExactMatch => &["title-1", "exact"],
            }},
            // #[watch]
            // remove_css_class?: { if !self.selected { Some("selected") } else { None }},
            // #[watch]
            // add_css_class?: { if self.selected { Some("selected") } else { None }},
            connect_clicked[sender, index] => move |_| {
                sender.output(LetterMsgOut::Selected(index)).unwrap();
            },
            #[watch]
            set_sensitive: (self.format == Format::Editable) && !self.selected
        }
    }

    fn init_model(value: Self::Init, index: &Coord, _sender: FactorySender<Self>) -> Self {
        Self {
            format: value.1,
            value: String::new(),
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
        }
    }
}
