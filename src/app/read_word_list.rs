use std::{
    collections::HashSet,
    env,
    fs::{self, File},
    io::{self, BufRead, BufReader},
};

use anyhow::{anyhow, Context};

use crate::onscreen_button::Key;

#[derive(Debug, PartialEq, Eq)]
pub(crate) struct WordList {
    pub allowed_words: HashSet<String>,
    pub allowed_letters: HashSet<char>,
    pub keys: Vec<Vec<Key>>,
    pub available_word_lengths: WordLengths,
}

#[derive(Debug, PartialEq, Eq, Default)]
pub(crate) struct WordLengths {
    pub l4: bool,
    pub l5: bool,
    pub l6: bool,
    pub l7: bool,
    pub l8: bool,
    pub l9: bool,
    pub l10: bool,
    pub l11: bool,
}

pub(crate) fn get_available_word_lists() -> anyhow::Result<Vec<String>> {
    let mut files = vec![];
    for entry in
        fs::read_dir(get_path()).map_err(|_| anyhow!("Could read directory '{}'", get_path()))?
    {
        if let Ok(entry) = entry {
            if entry.path().is_file() {
                if let Some(name) = entry.path().file_name() {
                    let name: String = name.to_string_lossy().into();
                    files.push(name.chars().take(name.chars().count() - 4).collect());
                }
            }
        }
    }
    Ok(files)
}

pub(crate) fn read_word_list(
    name_of_word_list: &str,
    word_length: usize,
) -> anyhow::Result<WordList> {
    let path = get_path();

    read_word_list_from_path(&(path.to_owned() + name_of_word_list + ".txt"), word_length)
}

fn get_path() -> &'static str {
    if env::var("FLATPAK_ID").is_ok() {
        "/app/share/word-lists/"
    } else {
        "data/resources/word-lists/"
    }
}

fn read_word_list_from_path(path: &str, length: usize) -> anyhow::Result<WordList> {
    let file = File::open(path).map_err(|_| anyhow!("Could not open '{path}'"))?;

    let mut lines = io::BufReader::new(file).lines();

    let (keys, allowed_letters) = get_keys_and_allowed_letters(&mut lines)?;

    let available_word_lengths =
        get_available_word_lengths(lines.next().context("File too short")??)?;

    let allowed_words = get_allowed_words(lines, length);

    Ok(WordList {
        allowed_words,
        allowed_letters,
        keys,
        available_word_lengths,
    })
}

fn get_available_word_lengths(line: String) -> anyhow::Result<WordLengths> {
    let mut lengths = WordLengths::default();

    let (prefix, numbers) = line
        .split_at_checked(9)
        .ok_or_else(|| anyhow!("String too short {line}"))?;
    if prefix != "4LENGTHS:" {
        Err(anyhow!("Expected prefix '4LENGTHS:', got '{}'", line))
    } else {
        for number in numbers.split(',') {
            match number {
                "4" => lengths.l4 = true,
                "5" => lengths.l5 = true,
                "6" => lengths.l6 = true,
                "7" => lengths.l7 = true,
                "8" => lengths.l8 = true,
                "9" => lengths.l9 = true,
                "10" => lengths.l10 = true,
                "11" => lengths.l11 = true,
                _ => return Err(anyhow!("Word length {number} not supported")),
            }
        }
        Ok(lengths)
    }
}

fn get_keys_and_allowed_letters(
    lines: &mut std::io::Lines<BufReader<std::fs::File>>,
) -> anyhow::Result<(Vec<Vec<Key>>, HashSet<char>)> {
    let mut rows = Vec::new();
    let mut allowed_letters = HashSet::new();

    for line_number in 1..4 {
        let line = lines.next().ok_or_else(|| anyhow!("File too short"))?;
        let (number, row) = line_to_keys(&line?)?;

        if line_number != number {
            return Err(anyhow!("Numbers of Keyboard Rows are not as expected",));
        }

        for key in &row {
            if let Key::Letter(c) = key {
                allowed_letters.insert(*c);
            }
        }
        rows.push(row);
    }

    Ok((rows, allowed_letters))
}

fn line_to_keys(line: &str) -> anyhow::Result<(usize, Vec<Key>)> {
    let mut keys = vec![];

    let (number, line) = line
        .split_at_checked(1)
        .ok_or_else(|| anyhow!("String too short {line}"))?;

    for key in line.split(',') {
        match key {
            "SEND" => keys.push(Key::Enter),
            "DEL" => keys.push(Key::Del),
            c => keys.push(Key::Letter(c.chars().next().expect("No Letter found."))),
        }
    }
    Ok((number.parse::<usize>()?, keys))
}

fn get_allowed_words(lines: io::Lines<io::BufReader<File>>, length: usize) -> HashSet<String> {
    lines.flatten().filter(|w| w.len() == length).collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test() {
        let actual_word_list = read_word_list_from_path("src/app/test.txt", 4).unwrap();
        let expected_word_list = WordList {
            allowed_words: vec!["AARE".to_owned(), "AALE".to_owned()]
                .into_iter()
                .collect(),
            allowed_letters: vec!['W', 'Y', 'Q', 'T', 'D', 'E', 'A', 'S', 'F', 'R']
                .into_iter()
                .collect(),
            keys: vec![
                vec![
                    Key::Letter('Q'),
                    Key::Letter('W'),
                    Key::Letter('E'),
                    Key::Letter('R'),
                    Key::Letter('T'),
                ],
                vec![
                    Key::Letter('A'),
                    Key::Letter('S'),
                    Key::Letter('D'),
                    Key::Letter('F'),
                ],
                vec![Key::Del, Key::Letter('Y'), Key::Enter],
            ],
            available_word_lengths: WordLengths {
                l4: true,
                l5: true,
                l6: true,
                ..Default::default()
            },
        };

        assert_eq!(actual_word_list, expected_word_list);
    }
}
