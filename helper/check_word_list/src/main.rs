mod word_list;
use std::{env, process::exit};

use word_list::read_word_list;

fn main() {
    let args: Vec<_> = env::args().collect();
    if args.len() != 2 {
        println!("One File name is expected as argument");
        exit(1);
    }
    let mut word_list = match read_word_list(&args[1]) {
        Ok(wl) => wl,
        Err(err) => {
            {
                println!("Error when reading wordlist: {err:?}")
            };
            exit(2);
        }
    };

    word_list.upper_case_lists();
    word_list.remove_duplicates();
    word_list.remove_words_with_illegal_word_lengths();
    word_list.check_for_illegal_letters();
    // word_list.check_secret_words_with_leo();
    word_list.print_statistics();
    word_list.store(&args[1]);
}
