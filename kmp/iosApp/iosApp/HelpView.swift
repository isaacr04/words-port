import SwiftUI
import shared

struct HelpView: View {
    @ObservedObject var viewModel: ObservableGameViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                // Header
                HStack {
                    Button(action: {
                        viewModel.processIntent(GameIntent.NavigateToGame())
                    }) {
                        Image(systemName: "chevron.left")
                            .font(.title2)
                    }
                    Spacer()
                    Text("How to Play")
                        .font(.title)
                        .fontWeight(.bold)
                    Spacer()
                    // Placeholder for alignment
                    Image(systemName: "chevron.left")
                        .font(.title2)
                        .opacity(0)
                }
                .padding()

                // Objective
                VStack(alignment: .leading, spacing: 8) {
                    Text("Objective")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(Color(red: 0.30, green: 0.69, blue: 0.31))

                    Text("Guess the secret word in 6 attempts or fewer. Each guess must be a valid word.")
                        .font(.system(size: 16))
                        .lineSpacing(6)
                }
                .padding(.horizontal)

                // How to Play
                VStack(alignment: .leading, spacing: 8) {
                    Text("How to Play")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(Color(red: 0.30, green: 0.69, blue: 0.31))

                    Text("1. Enter a word using the virtual keyboard")
                        .font(.system(size: 16))
                    Text("2. Press ENTER to submit your guess")
                        .font(.system(size: 16))
                    Text("3. The color of the tiles will change to show how close your guess was")
                        .font(.system(size: 16))
                        .lineSpacing(6)
                }
                .padding(.horizontal)

                // Examples
                VStack(alignment: .leading, spacing: 16) {
                    Text("Examples")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(Color(red: 0.30, green: 0.69, blue: 0.31))

                    ColorExample(
                        letter: "W",
                        color: Color(red: 0.30, green: 0.69, blue: 0.31),
                        explanation: "The letter W is in the word and in the correct position"
                    )

                    ColorExample(
                        letter: "I",
                        color: Color(red: 1.0, green: 0.76, blue: 0.03),
                        explanation: "The letter I is in the word but in the wrong position"
                    )

                    ColorExample(
                        letter: "U",
                        color: Color(red: 0.46, green: 0.46, blue: 0.46),
                        explanation: "The letter U is not in the word at all"
                    )
                }
                .padding(.horizontal)

                // Tips
                VStack(alignment: .leading, spacing: 8) {
                    Text("Tips")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(Color(red: 0.30, green: 0.69, blue: 0.31))

                    Text("• Start with common words that use different letters")
                        .font(.system(size: 16))
                        .lineSpacing(6)
                    Text("• Pay attention to the keyboard colors - they show which letters you've used")
                        .font(.system(size: 16))
                        .lineSpacing(6)
                    Text("• Letters can appear more than once in the same word")
                        .font(.system(size: 16))
                        .lineSpacing(6)
                }
                .padding(.horizontal)

                // Back Button
                Button(action: {
                    viewModel.processIntent(GameIntent.NavigateToGame())
                }) {
                    Text("Got it! Let's Play")
                        .font(.system(size: 16))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(red: 0.30, green: 0.69, blue: 0.31))
                        .cornerRadius(8)
                }
                .padding()
            }
        }
        .navigationBarHidden(true)
    }
}

struct ColorExample: View {
    let letter: String
    let color: Color
    let explanation: String

    var body: some View {
        HStack(spacing: 16) {
            // Letter tile
            ZStack {
                RoundedRectangle(cornerRadius: 4)
                    .fill(color)
                    .frame(width: 48, height: 48)

                Text(letter)
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(.white)
            }

            // Explanation
            Text(explanation)
                .font(.system(size: 14))
                .lineSpacing(4)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}
