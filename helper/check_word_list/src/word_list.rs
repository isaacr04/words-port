use std::{
    collections::{HashMap, HashSet},
    fs::File,
    io::{self, BufRead, BufReader, BufWriter, Write},
};

use anyhow::{Context, anyhow};

#[derive(Debug, PartialEq, Eq)]
pub enum Key {
    Letter(char),
    Enter,
    Del,
}

#[derive(Debug, PartialEq, Eq)]
pub(crate) struct WordList {
    pub secret_words: HashSet<String>,
    pub none_secret_words: HashSet<String>,
    pub allowed_letters: HashSet<char>,
    pub keys: Vec<Vec<Key>>,
    pub available_word_lengths: WordLengths,
}

impl WordList {
    pub fn upper_case_lists(&mut self) {
        self.secret_words = trim_and_capitalize_list(&self.secret_words);
        self.none_secret_words = trim_and_capitalize_list(&self.none_secret_words);
    }

    pub fn remove_duplicates(&mut self) {
        self.none_secret_words = self
            .none_secret_words
            .difference(&self.secret_words)
            .cloned()
            .collect();
    }

    pub fn check_for_illegal_letters(&self) {
        check_list_letters(&self.allowed_letters, &self.secret_words);
        check_list_letters(&self.allowed_letters, &self.none_secret_words);
    }

    pub fn remove_words_with_illegal_word_lengths(&mut self) {
        self.secret_words = filter_words_by_length(&mut self.secret_words);
        self.none_secret_words = filter_words_by_length(&mut self.none_secret_words);
    }

    pub(crate) fn print_statistics(&self) {
        let word_counter_secret = count_word_length(&self.secret_words);
        let word_counter_none_secret = count_word_length(&self.none_secret_words);

        let secret = word_counter_secret.keys().collect::<HashSet<_>>();
        let none_secret = word_counter_none_secret.keys().collect::<HashSet<_>>();
        let mut lengths: Vec<_> = secret.union(&none_secret).collect();
        lengths.sort();

        for length in lengths {
            println!(
                "Word length {}: Total number of words: {}, Number of secret words: {}",
                length,
                word_counter_secret.get(&length).unwrap_or(&0)
                    + word_counter_none_secret.get(&length).unwrap_or(&0),
                word_counter_secret.get(&length).unwrap_or(&0)
            );
        }
        println!(
            "In total {} words",
            word_counter_none_secret.values().sum::<usize>()
                + word_counter_secret.values().sum::<usize>()
        );
    }

    pub(crate) fn store(&self, name_of_word_list: &str) {
        let path = get_path();

        self.store_word(&(path.to_owned() + name_of_word_list + "_words.txt"));
    }

    fn store_word(&self, name_of_word_list: &str) {
        let file = File::create(name_of_word_list).expect("Unable to create file");
        let mut file = BufWriter::new(file);

        let mut secret_words: Vec<_> = self.secret_words.iter().collect();
        secret_words.sort();

        for w in secret_words {
            file.write(w.as_bytes()).unwrap();
            file.write("\n".as_bytes()).unwrap();
        }

        if !self.none_secret_words.is_empty() {
            file.write("----^^^^^-secret-^^^^----vvvv-public-vvvv----\n".as_bytes())
                .unwrap();

            let mut none_secret_words: Vec<_> = self.none_secret_words.iter().collect();
            none_secret_words.sort();

            for w in none_secret_words {
                file.write(w.as_bytes()).unwrap();
                file.write("\n".as_bytes()).unwrap();
            }
        }
    }

    pub(crate) fn check_secret_words_with_leo(&mut self) {
        let mut new_secret_words = HashSet::new();
        for word in &self.secret_words {
            print!("Checking word {word}: ");
            let response = reqwest::blocking::get(format!(
                "https://dict.leo.org/englisch-deutsch/{word}?side=left"
            ))
            .unwrap()
            .text()
            .unwrap();
            println!("{response}");
            if !response.contains("Es existiert derzeit auch keine Diskussion") {
                println!("Ok");
                new_secret_words.insert(word.clone());
            } else {
                println!("Not founs");
            }
        }
        self.secret_words = new_secret_words;
    }
}

fn count_word_length(list: &HashSet<String>) -> HashMap<usize, usize> {
    let mut counter = HashMap::new();
    for w in list {
        let length = w.chars().count();
        counter.entry(length).and_modify(|l| *l += 1).or_insert(1);
    }
    counter
}

#[test]
fn test_word_length_counter() {
    let actual = count_word_length(&HashSet::from([
        "Hause".to_owned(),
        "House".to_owned(),
        "nest".to_owned(),
    ]));
    let expected = HashMap::from([(5, 2), (4, 1)]);
    assert_eq!(actual, expected);
}

fn filter_words_by_length(list: &HashSet<String>) -> HashSet<String> {
    let mut new_list = HashSet::new();

    for w in list {
        let length = w.chars().count();
        if length >= 4 && length <= 11 {
            new_list.insert(w.clone());
        }
    }
    new_list
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

pub(crate) fn read_word_list(name_of_word_list: &str) -> anyhow::Result<WordList> {
    let path = get_path();

    read_word_list_from_path(&(path.to_owned() + name_of_word_list + ".txt"))
}

fn get_path() -> &'static str {
    "../../data/resources/word-lists/"
}

fn read_word_list_from_path(path: &str) -> anyhow::Result<WordList> {
    let file = File::open(path).map_err(|_| anyhow!("Could not open '{path}'"))?;

    let mut lines = io::BufReader::new(file).lines();

    let (keys, allowed_letters) = get_keys_and_allowed_letters(&mut lines)?;

    let available_word_lengths =
        get_available_word_lengths(lines.next().context("File too short")??)?;

    let (none_secret_words, secret_words) = get_words(lines);

    Ok(WordList {
        none_secret_words,
        secret_words,
        allowed_letters,
        keys,
        available_word_lengths,
    })
}

fn get_available_word_lengths(line: String) -> anyhow::Result<WordLengths> {
    let mut lengths = WordLengths::default();

    let (prefix, numbers) = line
        .split_at_checked(9)
        .ok_or_else(|| anyhow!("String too short for getting word lengths: '{line}'"))?;
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
        .ok_or_else(|| anyhow!("String too short when getting Keys: '{line}'"))?;

    for key in line.split(',') {
        match key {
            "SEND" => keys.push(Key::Enter),
            "DEL" => keys.push(Key::Del),
            c => keys.push(Key::Letter(c.chars().next().expect("No Letter found."))),
        }
    }
    Ok((number.parse::<usize>()?, keys))
}

fn get_words(mut lines: io::Lines<io::BufReader<File>>) -> (HashSet<String>, HashSet<String>) {
    let mut secrets = true;
    let mut none_secret_words = HashSet::new();
    let mut secret_words = HashSet::new();

    while let Some(Ok(line)) = lines.next() {
        if secrets == true && line.chars().next() == Some('-') {
            secrets = false;
            continue;
        }
        if secrets {
            secret_words.insert(line);
        } else {
            none_secret_words.insert(line.clone());
        }
    }
    (none_secret_words, secret_words)
}

fn trim_and_capitalize_list(list: &HashSet<String>) -> HashSet<String> {
    list.iter().map(|word| word.trim().to_uppercase()).collect()
}

fn check_list_letters(letters: &HashSet<char>, list: &HashSet<String>) {
    'word: for word in list {
        for c in word.chars() {
            if !letters.contains(&c) {
                println!("Word {word} contains illegal characters");
                continue 'word;
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test() {
        let actual_word_list = read_word_list_from_path("../../src/app/test.txt").unwrap();
        let expected_word_list = WordList {
            secret_words: vec![
                "ÄALE".to_owned(),
                "AAREAL".to_owned(),
                "AALFANG".to_owned(),
                "ABAKUS".to_owned(),
                "AALNETZ".to_owned(),
                "AALKORB".to_owned(),
            ]
            .into_iter()
            .collect(),
            none_secret_words: vec!["AARE".to_owned(), "BUCH".to_owned()]
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
