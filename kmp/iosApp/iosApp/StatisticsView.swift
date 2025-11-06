import SwiftUI
import shared

struct StatisticsView: View {
    @ObservedObject var viewModel: ObservableGameViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Header
                HStack {
                    Button(action: {
                        viewModel.processIntent(GameIntent.NavigateToGame())
                    }) {
                        Image(systemName: "chevron.left")
                            .font(.title2)
                    }
                    Spacer()
                    Text("Statistics")
                        .font(.title)
                        .fontWeight(.bold)
                    Spacer()
                    // Placeholder for alignment
                    Image(systemName: "chevron.left")
                        .font(.title2)
                        .opacity(0)
                }
                .padding()

                // Overall Statistics
                VStack(spacing: 16) {
                    Text("Overall Statistics")
                        .font(.system(size: 20, weight: .bold))

                    HStack(spacing: 24) {
                        StatColumn(
                            label: "Played",
                            value: "\(viewModel.state.statistics.gamesPlayed)"
                        )
                        StatColumn(
                            label: "Won",
                            value: "\(viewModel.state.statistics.gamesWon)"
                        )
                        StatColumn(
                            label: "Win %",
                            value: viewModel.state.statistics.gamesPlayed > 0
                                ? "\(viewModel.state.statistics.gamesWon * 100 / viewModel.state.statistics.gamesPlayed)%"
                                : "0%"
                        )
                    }

                    HStack(spacing: 24) {
                        StatColumn(
                            label: "Current Streak",
                            value: "\(viewModel.state.statistics.currentStreak)"
                        )
                        StatColumn(
                            label: "Max Streak",
                            value: "\(viewModel.state.statistics.maxStreak)"
                        )
                        StatColumn(
                            label: "Lost",
                            value: "\(viewModel.state.statistics.gamesLost)"
                        )
                    }
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)
                .padding(.horizontal)

                // Guess Distribution
                VStack(spacing: 16) {
                    Text("Guess Distribution")
                        .font(.system(size: 20, weight: .bold))

                    let maxCount = viewModel.state.statistics.guessDistribution.max() ?? 1

                    ForEach(0..<viewModel.state.statistics.guessDistribution.count, id: \.self) { index in
                        GuessDistributionBar(
                            attempt: index + 1,
                            count: Int(viewModel.state.statistics.guessDistribution[index]),
                            maxCount: Int(maxCount),
                            isCurrent: viewModel.state.attempts == index && viewModel.state.won
                        )
                    }
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)
                .padding(.horizontal)

                // Action Buttons
                VStack(spacing: 12) {
                    Button(action: {
                        viewModel.processIntent(GameIntent.NavigateToGame())
                    }) {
                        Text("Back to Game")
                            .font(.system(size: 16))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color(red: 0.30, green: 0.69, blue: 0.31))
                            .cornerRadius(8)
                    }

                    Button(action: {
                        viewModel.processIntent(GameIntent.StartNewGame())
                    }) {
                        Text("New Game")
                            .font(.system(size: 16))
                            .foregroundColor(Color(red: 0.30, green: 0.69, blue: 0.31))
                            .frame(maxWidth: .infinity)
                            .padding()
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(Color(red: 0.30, green: 0.69, blue: 0.31), lineWidth: 2)
                            )
                    }
                }
                .padding()
            }
        }
        .navigationBarHidden(true)
    }
}

struct StatColumn: View {
    let label: String
    let value: String

    var body: some View {
        VStack {
            Text(value)
                .font(.system(size: 32, weight: .bold))
                .foregroundColor(Color(red: 0.30, green: 0.69, blue: 0.31))
            Text(label)
                .font(.system(size: 14))
                .foregroundColor(.secondary)
        }
    }
}

struct GuessDistributionBar: View {
    let attempt: Int
    let count: Int
    let maxCount: Int
    let isCurrent: Bool

    var body: some View {
        HStack {
            Text("\(attempt)")
                .font(.system(size: 16, weight: .bold))
                .frame(width: 24)

            GeometryReader { geometry in
                let barWidth = maxCount > 0 ? CGFloat(count) / CGFloat(maxCount) * geometry.size.width : 0

                ZStack(alignment: .leading) {
                    Rectangle()
                        .fill(Color.clear)
                        .frame(width: geometry.size.width, height: 32)

                    Rectangle()
                        .fill(isCurrent ? Color(red: 0.30, green: 0.69, blue: 0.31) : Color(red: 0.30, green: 0.69, blue: 0.31).opacity(0.7))
                        .frame(width: max(barWidth, 40), height: 32)
                        .cornerRadius(4)

                    HStack {
                        Spacer()
                        Text("\(count)")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.trailing, 8)
                    }
                    .frame(width: max(barWidth, 40), height: 32)
                }
            }
            .frame(height: 32)
        }
    }
}
