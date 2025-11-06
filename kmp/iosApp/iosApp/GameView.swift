import SwiftUI
import shared

struct GameView: View {
    @ObservedObject var viewModel: ObservableGameViewModel

    var body: some View {
        VStack(spacing: 16) {
            // Header
            HStack {
                Spacer()
                Text("Words!")
                    .font(.title)
                    .fontWeight(.bold)
                Spacer()
                HStack(spacing: 12) {
                    Button(action: {
                        viewModel.processIntent(GameIntent.ShowStatistics())
                    }) {
                        Image(systemName: "chart.bar")
                    }
                    Button(action: {
                        viewModel.processIntent(GameIntent.ShowHelp())
                    }) {
                        Image(systemName: "questionmark.circle")
                    }
                    Button(action: {
                        viewModel.processIntent(GameIntent.ShowSettings())
                    }) {
                        Image(systemName: "gear")
                    }
                }
            }
            .padding()

            Spacer()

            // Game Grid
            VStack(spacing: 8) {
                ForEach(0..<viewModel.state.grid.count, id: \.self) { rowIndex in
                    HStack(spacing: 8) {
                        ForEach(0..<viewModel.state.grid[rowIndex].count, id: \.self) { colIndex in
                            LetterCell(letter: viewModel.state.grid[rowIndex][colIndex] as! Letter)
                        }
                    }
                }
            }
            .padding()

            Spacer()

            // Virtual Keyboard
            VStack(spacing: 6) {
                ForEach(0..<viewModel.state.keys.count, id: \.self) { rowIndex in
                    HStack(spacing: 4) {
                        ForEach(0..<viewModel.state.keys[rowIndex].count, id: \.self) { keyIndex in
                            KeyButton(
                                key: viewModel.state.keys[rowIndex][keyIndex] as! Key,
                                keyboardState: viewModel.state.keyboardState,
                                onKeyClick: { key in
                                    handleKeyPress(key)
                                }
                            )
                        }
                    }
                }
            }
            .padding()
        }
        .navigationBarHidden(true)
    }

    private func handleKeyPress(_ key: Key) {
        if let letterKey = key as? Key.Letter {
            viewModel.processIntent(GameIntent.EnterLetter(char: letterKey.char))
        } else if key is Key.Enter {
            viewModel.processIntent(GameIntent.EnterWord())
        } else if key is Key.Delete {
            viewModel.processIntent(GameIntent.Backspace())
        }
    }
}

struct LetterCell: View {
    let letter: Letter

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 4)
                .fill(backgroundColor)
                .frame(width: 56, height: 56)
                .overlay(
                    RoundedRectangle(cornerRadius: 4)
                        .stroke(borderColor, lineWidth: 2)
                )

            Text(letter.value)
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(textColor)
        }
    }

    private var backgroundColor: Color {
        switch letter.format {
        case .notUsed: return Color(red: 0.88, green: 0.88, blue: 0.88)
        case .noMatch: return Color(red: 0.46, green: 0.46, blue: 0.46)
        case .match: return Color(red: 1.0, green: 0.76, blue: 0.03)
        case .exactMatch: return Color(red: 0.30, green: 0.69, blue: 0.31)
        default: return Color.gray
        }
    }

    private var textColor: Color {
        letter.format == .notUsed ? .black : .white
    }

    private var borderColor: Color {
        if letter.selected {
            return Color(red: 0.30, green: 0.69, blue: 0.31)
        } else if letter.incorrect {
            return .red
        } else {
            return .clear
        }
    }
}

struct KeyButton: View {
    let key: Key
    let keyboardState: [Character: KeyFormat]
    let onKeyClick: (Key) -> Void

    var body: some View {
        Button(action: {
            onKeyClick(key)
        }) {
            Text(keyText)
                .font(.system(size: fontSize, weight: .bold))
                .foregroundColor(textColor)
                .frame(width: keyWidth, height: 48)
                .background(backgroundColor)
                .cornerRadius(4)
        }
    }

    private var keyText: String {
        if let letterKey = key as? Key.Letter {
            return String(letterKey.char)
        } else if key is Key.Enter {
            return "ENTER"
        } else if key is Key.Delete {
            return "⌫"
        }
        return ""
    }

    private var fontSize: CGFloat {
        key is Key.Letter ? 18 : 14
    }

    private var keyWidth: CGFloat {
        key is Key.Letter ? 36 : 60
    }

    private var backgroundColor: Color {
        if let letterKey = key as? Key.Letter {
            if let format = keyboardState[letterKey.char] {
                switch format {
                case .exactMatch: return Color(red: 0.30, green: 0.69, blue: 0.31)
                case .match: return Color(red: 1.0, green: 0.76, blue: 0.03)
                case .noMatch: return Color(red: 0.46, green: 0.46, blue: 0.46)
                case .unused: return Color(red: 0.88, green: 0.88, blue: 0.88)
                default: return Color(red: 0.88, green: 0.88, blue: 0.88)
                }
            }
            return Color(red: 0.88, green: 0.88, blue: 0.88)
        }
        return Color(red: 0.74, green: 0.74, blue: 0.74)
    }

    private var textColor: Color {
        if let letterKey = key as? Key.Letter {
            if let format = keyboardState[letterKey.char] {
                switch format {
                case .exactMatch, .match, .noMatch: return .white
                default: return .black
                }
            }
        }
        return .black
    }
}
