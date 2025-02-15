use relm4::{
    adw::{self, prelude::AdwDialogExt},
    gtk::{
        self,
        prelude::{ButtonExt, GridExt, OrientableExt, WidgetExt},
        Align,
    },
    ComponentParts, ComponentSender, RelmWidgetExt, SimpleComponent,
};

#[derive(Debug, Clone)]
pub(crate) struct HelpDialog {}

#[relm4::component(pub)]
impl SimpleComponent for HelpDialog {
    type Init = HelpDialog;
    type Input = ();
    type Output = ();

    view! {
        #[root]
        adw::Dialog {
            set_follows_content_size: true,
            set_can_close: true,
            #[wrap(Some)]
            set_child = &gtk::Box{
                set_orientation: gtk::Orientation::Vertical,
                set_hexpand: true,
                set_vexpand: true,

                adw::HeaderBar {
                    #[wrap(Some)]
                    set_title_widget = &gtk::Label {
                        set_label: "Help",
                    }
                },
                adw::Clamp{
                    gtk::Box{
                        set_hexpand: true,
                        set_vexpand: true,
                        set_align: Align::Fill,
                        set_orientation: gtk::Orientation::Vertical,

                        gtk::Label {
                            set_margin_top: 20,
                            set_margin_start: 5,
                            set_margin_end: 5,
                            set_wrap: true,
                            set_justify: gtk::Justification::Center,
                            set_markup: "<b>Words!</b> is a word puzzle where you have <b>six</b> tries to guess a hidden word.",
                        },
                        gtk::Label {
                            #[watch]
                            set_margin_top: 10,
                            set_margin_start: 5,
                            set_margin_end: 5,
                            set_wrap: true,
                            set_justify: gtk::Justification::Center,
                            set_markup: "Each guess must be a <b>valid</b> word.",
                        },
                        gtk::Label {
                            set_margin_top: 10,
                            set_margin_start: 5,
                            set_margin_end: 5,
                            set_wrap: true,
                            set_justify: gtk::Justification::Center,
                            set_markup: "After each guess, the game provides feedback with <b>color-coded</b> hints",
                        },
                        gtk::Grid {
                            set_align: Align::Center,
                            set_margin_all: 10,
                            set_row_homogeneous: true,
                            set_column_spacing: 5,

                            attach[1, 1, 1, 1] = &gtk::Button {
                                set_label: &"A",
                                set_css_classes: &["exact"],
                                set_halign: Align::End,
                                set_valign: Align::Center,
                            },
                            attach[2, 1, 1, 1] = &gtk::Label {
                                set_label: "The letter is correct and in the right position.",
                                set_wrap: true,
                                set_justify: gtk::Justification::Left,
                                set_xalign: 0.0,
                                set_valign: Align::Center,
                                set_hexpand: true
                            },
                            attach[1, 2, 1, 1] = &gtk::Button {
                                set_label: &"B",
                                set_css_classes: &["match"],
                                set_halign: Align::End,
                                set_valign: Align::Center,
                            },
                            attach[2, 2, 1, 1] =  &gtk::Label {
                                set_label: "The letter is correct but in the wrong position.",
                                set_wrap: true,
                                set_justify: gtk::Justification::Left,
                                set_xalign: 0.0,
                                set_valign: Align::Center,
                                set_hexpand: true
                            },
                            attach[1, 3, 1, 1] = &gtk::Button {
                                set_label: &"C",
                                set_css_classes: &["no_match"],
                                set_halign: Align::End,
                                set_valign: Align::Center,
                            },
                            attach[2, 3, 1, 1] = &gtk::Label {
                                set_label: "The letter isn’t in the word.",
                                set_wrap: true,
                                set_justify: gtk::Justification::Left,
                                set_xalign: 0.0,
                                set_valign: Align::Center,
                                set_hexpand: true
                            }
                        },
                        gtk::Label {
                            set_label: "Use this feedback to refine your subsequent guesses and solve the puzzle within six attempts.",
                            set_justify: gtk::Justification::Center,
                            set_margin_top: 5,
                            set_margin_bottom: 5,
                            set_wrap: true,
                        }
                    }
                }
            }
        }
    }

    fn init(
        _init: Self::Init,
        root: Self::Root,
        _sender: ComponentSender<Self>,
    ) -> ComponentParts<Self> {
        let model = HelpDialog {};
        let widgets = view_output!();

        ComponentParts { model, widgets }
    }
}
