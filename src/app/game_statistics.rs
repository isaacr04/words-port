use std::{
    env,
    fs::File,
    io::{Read, Write},
};

use serde::{Deserialize, Serialize};

use super::TRIES;

#[derive(Serialize, Deserialize, Default)]
pub struct GameStatistics {
    pub total_games: usize,
    pub games_won: usize,
    pub current_streak: usize,
    pub longest_streak: usize,
    pub games_won_tries: Vec<usize>,
}

pub enum Outcome {
    Won(usize),
    Lost,
}

impl GameStatistics {
    pub fn update_game_statistics(
        &mut self,
        outcome: Outcome,
        game_name: &str,
        number_of_letters: usize,
    ) -> anyhow::Result<()> {
        self.total_games += 1;
        if let Outcome::Won(tries) = outcome {
            self.games_won += 1;
            self.current_streak += 1;
            self.longest_streak = self.longest_streak.max(self.current_streak);
            self.games_won_tries[tries - 1] += 1;
        } else {
            self.current_streak = 0;
        }
        self.save_statistics(game_name, number_of_letters)?;
        Ok(())
    }

    fn save_statistics(&self, name: &str, number_of_letters: usize) -> anyhow::Result<()> {
        let file = get_path() + name + "_" + &number_of_letters.to_string();

        let mut output = File::create(file)?;
        output.write_all(serde_json::to_string_pretty(self)?.as_bytes())?;
        Ok(())
    }

    pub fn load_game_statistics(name: &str, number_of_letters: usize) -> Self {
        let name = get_path() + name + "_" + &number_of_letters.to_string();

        if let Ok(statistics) = load_statistics_from_file(name) {
            statistics
        } else {
            GameStatistics {
                games_won_tries: vec![0; TRIES],
                ..Default::default()
            }
        }
    }
}

fn load_statistics_from_file(name: String) -> Result<GameStatistics, anyhow::Error> {
    let mut f = File::open(&name)?;
    let mut content = String::new();
    f.read_to_string(&mut content)?;
    let statistic = serde_json::from_str(&content)?;
    Ok(statistic)
}

fn get_path() -> String {
    env::var("XDG_DATA_HOME").expect("XDG_DATA_HOME not found") + "/"
}
