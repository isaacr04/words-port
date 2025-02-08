use relm4::{
    adw::{self, prelude::AdwDialogExt},
    gtk::{
        self,
        prelude::{BoxExt, OrientableExt},
    },
    ComponentParts, ComponentSender, RelmWidgetExt, SimpleComponent,
};

pub struct StatisticsDialog {}

#[relm4::component(pub)]
impl SimpleComponent for StatisticsDialog {
    type Init = ();
    type Input = ();
    type Output = ();

    view! {
    #[root]
    adw::Dialog {
        #[wrap(Some)]
        set_child = &gtk::Box {
                set_orientation: gtk::Orientation::Vertical,
                set_spacing: 10,
                set_margin_all: 20,

                gtk::Label {
                    set_label: "This is an Adwaita modal dialog.",
                    set_wrap: true,
                }
            }

        }
    }

    fn init(
        _init: Self::Init,
        root: Self::Root,
        _sender: ComponentSender<Self>,
    ) -> ComponentParts<Self> {
        let model = StatisticsDialog {};
        let widgets: StatisticsDialogWidgets = view_output!();
        ComponentParts { model, widgets }
    }
}
