import SwiftUI
import shared

struct SettingsView: View {
    @ObservedObject var viewModel: ObservableGameViewModel
    @State private var showWordListPicker = false
    @State private var showWordLengthPicker = false
    @State private var showClearAlert = false

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
                    Text("Settings")
                        .font(.title)
                        .fontWeight(.bold)
                    Spacer()
                    // Placeholder for alignment
                    Image(systemName: "chevron.left")
                        .font(.title2)
                        .opacity(0)
                }
                .padding()

                // Game Settings
                VStack(alignment: .leading, spacing: 16) {
                    Text("Game Settings")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(Color(red: 0.30, green: 0.69, blue: 0.31))
                        .padding(.horizontal)

                    Button(action: {
                        showWordListPicker = true
                    }) {
                        SettingItem(
                            title: "Word List",
                            value: viewModel.state.wordListName
                        )
                    }

                    Button(action: {
                        showWordLengthPicker = true
                    }) {
                        SettingItem(
                            title: "Word Length",
                            value: "\(viewModel.state.wordLength) letters"
                        )
                    }
                }

                // Statistics
                VStack(alignment: .leading, spacing: 16) {
                    Text("Statistics")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(Color(red: 0.30, green: 0.69, blue: 0.31))
                        .padding(.horizontal)

                    Button(action: {
                        viewModel.processIntent(GameIntent.ShowStatistics())
                    }) {
                        Text("View Statistics")
                            .font(.system(size: 16))
                            .foregroundColor(Color(red: 0.30, green: 0.69, blue: 0.31))
                            .frame(maxWidth: .infinity)
                            .padding()
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(Color(red: 0.30, green: 0.69, blue: 0.31), lineWidth: 2)
                            )
                    }
                    .padding(.horizontal)

                    Button(action: {
                        showClearAlert = true
                    }) {
                        Text("Clear Statistics")
                            .font(.system(size: 16))
                            .foregroundColor(.red)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(.red, lineWidth: 2)
                            )
                    }
                    .padding(.horizontal)
                }

                // About
                VStack(alignment: .leading, spacing: 16) {
                    Text("About")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(Color(red: 0.30, green: 0.69, blue: 0.31))

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Words! - Kotlin Multiplatform")
                            .font(.system(size: 16, weight: .bold))
                        Text("A word guessing game where you have 6 attempts to guess the secret word.")
                            .font(.system(size: 14))
                            .foregroundColor(.secondary)
                            .lineSpacing(4)
                        Text("Version 1.0.0")
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                }
                .padding(.horizontal)
            }
        }
        .navigationBarHidden(true)
        .sheet(isPresented: $showWordListPicker) {
            WordListPicker(
                wordLists: Array(viewModel.state.availableWordLists),
                selectedWordList: viewModel.state.wordListName,
                onSelect: { wordList in
                    viewModel.processIntent(GameIntent.SelectWordList(name: wordList))
                    showWordListPicker = false
                }
            )
        }
        .sheet(isPresented: $showWordLengthPicker) {
            WordLengthPicker(
                wordLengths: Array(viewModel.state.availableWordLengths.map { Int($0) }),
                selectedWordLength: Int(viewModel.state.wordLength),
                onSelect: { length in
                    viewModel.processIntent(GameIntent.SelectWordLength(length: Int32(length)))
                    showWordLengthPicker = false
                }
            )
        }
        .alert("Clear Statistics?", isPresented: $showClearAlert) {
            Button("Cancel", role: .cancel) { }
            Button("Clear", role: .destructive) {
                viewModel.processIntent(GameIntent.ClearStatistics())
            }
        } message: {
            Text("This will permanently delete all your game statistics. This action cannot be undone.")
        }
    }
}

struct SettingItem: View {
    let title: String
    let value: String

    var body: some View {
        HStack {
            Text(title)
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(.primary)
            Spacer()
            Text(value)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Color(red: 0.30, green: 0.69, blue: 0.31))
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(8)
        .padding(.horizontal)
    }
}

struct WordListPicker: View {
    let wordLists: [String]
    let selectedWordList: String
    let onSelect: (String) -> Void

    var body: some View {
        NavigationView {
            List(wordLists, id: \.self) { wordList in
                Button(action: {
                    onSelect(wordList)
                }) {
                    HStack {
                        Text(wordList)
                        Spacer()
                        if wordList == selectedWordList {
                            Image(systemName: "checkmark")
                                .foregroundColor(Color(red: 0.30, green: 0.69, blue: 0.31))
                        }
                    }
                }
            }
            .navigationTitle("Select Word List")
        }
    }
}

struct WordLengthPicker: View {
    let wordLengths: [Int]
    let selectedWordLength: Int
    let onSelect: (Int) -> Void

    var body: some View {
        NavigationView {
            List(wordLengths, id: \.self) { length in
                Button(action: {
                    onSelect(length)
                }) {
                    HStack {
                        Text("\(length) letters")
                        Spacer()
                        if length == selectedWordLength {
                            Image(systemName: "checkmark")
                                .foregroundColor(Color(red: 0.30, green: 0.69, blue: 0.31))
                        }
                    }
                }
            }
            .navigationTitle("Select Word Length")
        }
    }
}
