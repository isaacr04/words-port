mod app;
mod config;
mod letter;
mod modals;
mod onscreen_button;

use crate::config::{GETTEXT_PACKAGE, LOCALEDIR};
use app::{App, HelpAction, NewGameAction, StatisticsAction};
use config::{APP_ID, RESOURCES_FILE};
use gettextrs::{LocaleCategory, bindtextdomain, setlocale, textdomain};
use gtk::prelude::ApplicationExt;
use gtk::{gio, glib};
use relm4::set_global_css;
use relm4::{
    RelmApp,
    actions::{AccelsPlus, RelmAction, RelmActionGroup},
    gtk, main_application,
};
use tr::{tr, tr_init};

relm4::new_action_group!(AppActionGroup, "app");
relm4::new_stateless_action!(QuitAction, AppActionGroup, "quit");

fn main() {
    gtk::init().unwrap();

    tr_init!(LOCALEDIR);
    setlocale(LocaleCategory::LcAll, "");
    let _ = bindtextdomain(GETTEXT_PACKAGE, LOCALEDIR);
    let _ = textdomain(GETTEXT_PACKAGE);

    glib::set_application_name(&tr!("Words!"));

    let res = gio::Resource::load(RESOURCES_FILE).expect("Could not load gresource file");
    gio::resources_register(&res);

    gtk::Window::set_default_icon_name(APP_ID);

    let app = main_application();
    app.set_resource_base_path(Some("/page/codeberg/petsoi/words/"));
    app.set_application_id(Some(APP_ID));

    let mut actions = RelmActionGroup::<AppActionGroup>::new();

    let quit_action = {
        let app = app.clone();
        RelmAction::<QuitAction>::new_stateless(move |_| {
            app.quit();
        })
    };

    actions.add_action(quit_action);
    actions.register_for_main_application();

    app.set_accelerators_for_action::<QuitAction>(&["<Control>q"]);
    app.set_accelerators_for_action::<NewGameAction>(&["<Control>n"]);
    app.set_accelerators_for_action::<StatisticsAction>(&["<Control>s"]);
    app.set_accelerators_for_action::<HelpAction>(&["<Control>h"]);

    let app = RelmApp::from_app(app);

    let data = res
        .lookup_data(
            "/page/codeberg/petsoi/words/style.css",
            gio::ResourceLookupFlags::NONE,
        )
        .unwrap();
    set_global_css(&glib::GString::from_utf8_checked(data.to_vec()).unwrap());
    let data = res
        .lookup_data(
            "/page/codeberg/petsoi/words/style-dark.css",
            gio::ResourceLookupFlags::NONE,
        )
        .unwrap();
    set_global_css(&glib::GString::from_utf8_checked(data.to_vec()).unwrap());

    app.visible_on_activate(false).run::<App>(());
}
