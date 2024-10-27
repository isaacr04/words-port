use relm4::{
    factory::{positions::GridPosition, Position},
    gtk::{self, prelude::ButtonExt, prelude::WidgetExt},
    prelude::{DynamicIndex, FactoryComponent},
    FactorySender, RelmWidgetExt,
};

#[derive(Debug)]
pub enum Format {
    Entered,
    NoMatch,
    Match,
    ExactMatch,
}

#[derive(Debug)]
pub struct Letter {
    pub value: String,
    pub format: Format,
    pub width: usize,
}

#[derive(Debug)]
pub enum LetterMsgOut {
    Selected(DynamicIndex),
}

#[derive(Debug)]
pub enum LetterMsgIn {
    SetContent(Option<char>),
    SetFormat(Format),
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
    type Init = usize;
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
            add_css_class: "title-1",
            #[watch]
            add_css_class?: { match &self.format {
                Format::Entered => None,
                Format::NoMatch  => Some("no_match"),
                Format::Match  => Some("exact"),
                Format::ExactMatch => Some("exact"),
            }},
            connect_clicked[sender, index] => move |_| {
                sender.output(LetterMsgOut::Selected(index.clone())).unwrap();
            }
        }
    }

    fn init_model(value: Self::Init, _index: &DynamicIndex, _sender: FactorySender<Self>) -> Self {
        Self {
            format: Format::Entered,
            value: "A".to_string(), //String::new(),
            width: value,
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
        }
    }
}
