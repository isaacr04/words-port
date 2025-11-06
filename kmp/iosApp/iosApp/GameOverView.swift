import SwiftUI
import shared

struct GameOverView: View {
    @ObservedObject var viewModel: ObservableGameViewModel

    var body: some View {
        VStack(spacing: 24) {
            Text("Game Over")
                .font(.title)
                .fontWeight(.bold)
                .padding(.top)

            Spacer()

            // Result
            Text(viewModel.state.won ? "Victory! 🎉" : "Game Over")
                .font(.system(size: 32, weight: .bold))
                .foregroundColor(viewModel.state.won ? Color(red: 0.30, green: 0.69, blue: 0.31) : .red)

            // Word Display
            VStack(spacing: 8) {
                Text("The word was:")
                    .font(.system(size: 18))
                    .foregroundColor(.secondary)

                Text(viewModel.state.word.uppercased())
                    .font(.system(size: 36, weight: .bold))
                    .foregroundColor(Color(red: 0.30, green: 0.69, blue: 0.31))
            }

            // Statistics Summary
            VStack(spacing: 16) {
                Text("Statistics")
                    .font(.system(size: 20, weight: .bold))

                HStack(spacing: 32) {
                    StatItem(
                        label: "Played",
                        value: "\(viewModel.state.statistics.gamesPlayed)"
                    )
                    StatItem(
                        label: "Win %",
                        value: viewModel.state.statistics.gamesPlayed > 0
                            ? "\(viewModel.state.statistics.gamesWon * 100 / viewModel.state.statistics.gamesPlayed)%"
                            : "0%"
                    )
                    StatItem(
                        label: "Streak",
                        value: "\(viewModel.state.statistics.currentStreak)"
                    )
                }
            }
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(12)
            .padding(.horizontal)

            Spacer()

            // Action Buttons
            VStack(spacing: 12) {
                Button(action: {
                    viewModel.processIntent(GameIntent.StartNewGame())
                }) {
                    Text("Play Again")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(red: 0.30, green: 0.69, blue: 0.31))
                        .cornerRadius(8)
                }

                Button(action: {
                    viewModel.processIntent(GameIntent.ShowStatistics())
                }) {
                    Text("View Detailed Statistics")
                        .font(.system(size: 16))
                        .foregroundColor(Color(red: 0.30, green: 0.69, blue: 0.31))
                        .frame(maxWidth: .infinity)
                        .padding()
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(Color(red: 0.30, green: 0.69, blue: 0.31), lineWidth: 2)
                        )
                }

                Button(action: {
                    viewModel.processIntent(GameIntent.ShowSettings())
                }) {
                    Text("Change Settings")
                        .font(.system(size: 16))
                        .foregroundColor(.blue)
                }
            }
            .padding()
        }
        .navigationBarHidden(true)
    }
}

struct StatItem: View {
    let label: String
    let value: String

    var body: some View {
        VStack {
            Text(value)
                .font(.system(size: 28, weight: .bold))
                .foregroundColor(Color(red: 0.30, green: 0.69, blue: 0.31))
            Text(label)
                .font(.system(size: 14))
                .foregroundColor(.secondary)
        }
    }
}
