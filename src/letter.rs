use relm4::{
    factory::{positions::GridPosition, Position},
    gtk::{self, prelude::ButtonExt, prelude::WidgetExt},
    prelude::{DynamicIndex, FactoryComponent},
    FactorySender, RelmWidgetExt,
};

#[derive(Debug, PartialEq)]
pub enum Format {
    Editable,
    NotUsed,
    NoMatch,
    Match,
    ExactMatch,
}

#[derive(Debug)]
pub struct Letter {
    value: String,
    format: Format,
    width: usize,
    selected: bool,
}

#[derive(Debug)]
pub enum LetterMsgOut {
    Selected(DynamicIndex),
}

#[derive(Debug)]
pub enum LetterMsgIn {
    SetContent(Option<String>),
    SetFormat(Format),
    SetSelected(bool),
}

impl Position<GridPosition, DynamicIndex> for Letter {
    fn position(&self, index: &DynamicIndex) -> GridPosition {
        let index = index.current_index();
        let x = index % self.width as usize;
        let y = index / self.width as usize;
        GridPosition {
            column: x as i32,
            row: y as i32,
            width: 1,
            height: 1,
        }
    }
}

#[relm4::factory(pub)]
impl FactoryComponent for Letter {
    type Init = (usize, Format);
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
                sender.output(LetterMsgOut::Selected(index.clone())).unwrap();
            },
            #[watch]
            set_sensitive: (self.format == Format::Editable) && !self.selected
        }
    }

    fn init_model(value: Self::Init, index: &DynamicIndex, _sender: FactorySender<Self>) -> Self {
        Self {
            format: value.1,
            value: " ".to_string(), //String::new(),
            width: value.0,
            selected: if index.current_index() == 0 {
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
