use relm4::{
    gtk::{
        self,
        prelude::{ButtonExt, WidgetExt},
    },
    prelude::FactoryComponent,
    FactorySender, RelmWidgetExt,
};

#[derive(Debug, PartialEq, Copy, Clone, PartialOrd)]
pub enum Format {
    NotUsed = 0,
    NoMatch = 1,
    Match = 2,
    ExactMatch = 3,
}

#[derive(Debug)]
pub struct OnScreenButton {
    pub trigger: OnScreenButtonMsgOut,
    pub format: Format,
}

#[derive(Debug)]
pub enum OnScreenButtonMsgIn {
    SetFormat(Format),
    Reset,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum OnScreenButtonMsgOut {
    Letter(char),
    Enter,
    Del,
}

impl OnScreenButtonMsgOut {
    fn get_label(&self) -> String {
        match self {
            OnScreenButtonMsgOut::Letter(c) => c.to_uppercase().to_string(),
            OnScreenButtonMsgOut::Enter => "Enter".to_owned(),
            OnScreenButtonMsgOut::Del => "Delete".to_owned(),
        }
    }
}

#[relm4::factory(pub)]
impl FactoryComponent for OnScreenButton {
    type Init = OnScreenButtonMsgOut;
    type Index = OnScreenButtonMsgOut;
    type Input = OnScreenButtonMsgIn;
    type Output = OnScreenButtonMsgOut;
    type CommandOutput = ();
    type ParentWidget = gtk::Box;

    // = help: implement the missing item: `type Root = /* Type */;`
    // = help: implement the missing item: `fn init_root(&self) -> <Self as FactoryComponent>::Root { todo!() }`
    // = help: implement the missing item: `fn init_widgets(&mut self, _: &<Self as FactoryComponent>::Index, _: <Self as FactoryComponent>::Root, _: &<<Self as FactoryComponent>::ParentWidget as FactoryView>::ReturnedWidget, _: FactorySender<Self>) -> <Self as FactoryComponent>::Widgets { todo!() }`

    view! {
        #[root]
        #[name(button)]
        gtk::Button {
            set_margin_all: 1,
            set_has_frame: true,
            #[watch]
            set_label: &self.trigger.get_label(),
            #[watch]
            set_css_classes: { match &self.format {
                Format::NotUsed => &[],
                Format::NoMatch => &["no_match"],
                Format::Match => &["match"],
                Format::ExactMatch => &["exact"],
            }},
            connect_clicked[sender, index] =>
                move |_| {
                    println!("Letter test");
                    sender.output(index).unwrap();
                }
        }
    }

    fn init_model(
        value: Self::Init,
        _index: &OnScreenButtonMsgOut,
        _sender: FactorySender<Self>,
    ) -> Self {
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
            OnScreenButtonMsgIn::Reset => self.format = Format::NotUsed,
        }
    }
}
