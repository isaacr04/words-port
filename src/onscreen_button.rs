use relm4::{
    FactorySender, RelmWidgetExt,
    gtk::{
        self,
        prelude::{ButtonExt, WidgetExt},
    },
    prelude::FactoryComponent,
};
use tr::tr;

#[derive(Debug, PartialEq, Copy, Clone, PartialOrd)]
pub enum Format {
    NotUsed = 0,
    NoMatch = 1,
    Match = 2,
    ExactMatch = 3,
}

#[derive(Debug)]
pub struct OnScreenButton {
    pub trigger: Key,
    pub format: Format,
}

#[derive(Debug)]
pub enum OnScreenButtonMsgIn {
    SetFormat(Format),
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Key {
    Letter(char),
    Enter,
    Del,
}

impl Key {
    fn get_label(&self) -> String {
        match self {
            Key::Letter(c) => c.to_uppercase().to_string(),
            Key::Enter => tr!("Enter"),
            Key::Del => tr!("Del"),
        }
    }
}

#[relm4::factory(pub)]
impl FactoryComponent for OnScreenButton {
    type Init = Key;
    type Index = Key;
    type Input = OnScreenButtonMsgIn;
    type Output = Key;
    type CommandOutput = ();
    type ParentWidget = gtk::Box;

    view! {
        #[root]
        #[name(button)]
        gtk::Button {
            set_margin_all: 1,
            set_hexpand: true,
            set_vexpand: true,
            set_label: &self.trigger.get_label(),
            #[watch]
            set_css_classes: { match &self.format {
                Format::NotUsed => &["osk"],
                Format::NoMatch => &["no_match", "osk"],
                Format::Match => &["match", "osk"],
                Format::ExactMatch => &["exact", "osk"],
            }},
            connect_clicked[sender, index] =>
                move |_| {
                    sender.output(index).unwrap();
                },
            set_can_focus: false
        }
    }

    fn init_model(value: Self::Init, _index: &Key, _sender: FactorySender<Self>) -> Self {
        Self {
            format: Format::NotUsed,
            trigger: value.into(),
        }
    }

    fn update(&mut self, msg: Self::Input, _sender: FactorySender<Self>) {
        match msg {
            OnScreenButtonMsgIn::SetFormat(f) => {
                if f > self.format {
                    self.format = f
                }
            }
        }
    }
}
