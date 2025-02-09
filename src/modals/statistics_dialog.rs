use relm4::{
    adw::{self, prelude::AdwDialogExt},
    gtk::{
        self,
        prelude::{OrientableExt, WidgetExt},
    },
    ComponentParts, ComponentSender, RelmWidgetExt, SimpleComponent,
};

pub(crate) use crate::app::game_statistics::GameStatistics;

#[derive(Debug)]
pub(crate) enum StatisticsMsg {
    Update(StatisticsDialog),
}

#[derive(Debug, Clone)]
pub(crate) struct StatisticsDialog {
    pub statistic: GameStatistics,
    pub word_list_name: String,
    pub number_of_letters: usize,
}

#[relm4::component(pub)]
impl SimpleComponent for StatisticsDialog {
    type Init = StatisticsDialog;
    type Input = StatisticsMsg;
    type Output = ();

    view! {
    #[root]
    adw::Dialog {
        #[wrap(Some)]
        set_child = &gtk::Box {
                set_orientation: gtk::Orientation::Vertical,
                set_hexpand: true,
                set_vexpand: true,

                adw::HeaderBar {
                    #[wrap(Some)]
                    set_title_widget = &gtk::Label {
                        set_label: "Statistics",
                    }
                },

                gtk::Box {
                    set_margin_all: 20,
                    set_orientation: gtk::Orientation::Vertical,

                    gtk::Label {
                        #[watch]
                        set_label: &format!("{} - Word Length: {}", model.word_list_name, model.number_of_letters),
                        set_css_classes: &["title-3"],
                        set_wrap: true,
                    },
                    gtk::Label {
                        #[watch]
                        set_label: &format!("You won {} out of {} games.", model.statistic.games_won, model.statistic.total_games),
                        set_wrap: true,
                        set_margin_top: 10,
                    },
                    gtk::Label {
                        #[watch]
                        set_label: &format!("Streaks: Current: {}, Longest: {}", model.statistic.current_streak,  model.statistic.longest_streak),
                        set_wrap: true,
                        set_margin_top: 10,
                    },
                    gtk::Label {
                        set_label: "Distribution by Trials:",
                        set_wrap: true,
                        set_css_classes: &["title-3"],
                        set_margin_top: 20,
                    },
                    gtk::Box {
                        set_orientation: gtk::Orientation::Horizontal,
                        set_align: gtk::Align::Center,
                        set_margin_top: 10,
                        set_margin_bottom: 5,
                        gtk::Label {
                            set_label: "1",
                            set_margin_end: 10,
                            },
                        gtk::ProgressBar {
                            set_align: gtk::Align::BaselineCenter,
                            #[watch]
                            set_fraction: model.statistic.games_won_tries[0] as f64 / model.statistic.games_won as f64
                        }

                    },
                    gtk::Box {
                        set_orientation: gtk::Orientation::Horizontal,
                        set_align: gtk::Align::Center,
                        set_margin_bottom: 5,
                        gtk::Label {
                            set_label: "2",
                            set_margin_end: 10,
                            set_align: gtk::Align::End,
                            },
                        gtk::ProgressBar {
                            set_align: gtk::Align::BaselineCenter,
                            #[watch]
                            set_fraction: model.statistic.games_won_tries[1] as f64 / model.statistic.games_won as f64
                        }

                    },
                    gtk::Box {
                        set_orientation: gtk::Orientation::Horizontal,
                        set_align: gtk::Align::Center,
                        set_margin_bottom: 5,
                        gtk::Label {
                            set_label: "3",
                            set_margin_end: 10,
                            set_align: gtk::Align::End,
                            },
                        gtk::ProgressBar {
                            set_align: gtk::Align::BaselineCenter,
                            #[watch]
                            set_fraction: model.statistic.games_won_tries[2] as f64 / model.statistic.games_won as f64
                        }

                    },
                    gtk::Box {
                        set_orientation: gtk::Orientation::Horizontal,
                        set_align: gtk::Align::Center,
                        set_margin_bottom: 5,
                        gtk::Label {
                            set_label: "4",
                            set_margin_end: 10,
                            set_align: gtk::Align::End,
                            },
                        gtk::ProgressBar {
                            set_align: gtk::Align::BaselineCenter,
                            #[watch]
                            set_fraction: model.statistic.games_won_tries[3] as f64 / model.statistic.games_won as f64
                        }

                    },
                    gtk::Box {
                        set_orientation: gtk::Orientation::Horizontal,
                        set_align: gtk::Align::Center,
                        set_margin_bottom: 5,
                        gtk::Label {
                            set_label: "5",
                            set_margin_end: 10,
                            set_align: gtk::Align::End,
                            },
                        gtk::ProgressBar {
                            set_align: gtk::Align::BaselineCenter,
                            #[watch]
                            set_fraction: model.statistic.games_won_tries[4] as f64 / model.statistic.games_won as f64
                        }

                    },
                    gtk::Box {
                        set_orientation: gtk::Orientation::Horizontal,
                        set_align: gtk::Align::Center,
                        gtk::Label {
                            set_label: "6",
                            set_margin_end: 10,
                            set_align: gtk::Align::End,
                            },
                        gtk::ProgressBar {
                            set_align: gtk::Align::BaselineCenter,
                            #[watch]
                            set_fraction: model.statistic.games_won_tries[5] as f64 / model.statistic.games_won as f64
                        }
                    },
                }
            }

        }
    }

    fn update(&mut self, message: Self::Input, _sender: ComponentSender<Self>) {
        match message {
            StatisticsMsg::Update(statistics_dialog) => *self = statistics_dialog,
        }
    }

    fn init(
        init: Self::Init,
        root: Self::Root,
        _sender: ComponentSender<Self>,
    ) -> ComponentParts<Self> {
        let model = init;
        let widgets: StatisticsDialogWidgets = view_output!();
        ComponentParts { model, widgets }
    }
}
