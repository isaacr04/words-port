import SwiftUI
import shared

struct ContentView: View {
    @StateObject private var viewModel = ObservableGameViewModel()

    var body: some View {
        NavigationView {
            ZStack {
                switch viewModel.state.page {
                case .game:
                    GameView(viewModel: viewModel)
                case .gameOver:
                    GameOverView(viewModel: viewModel)
                case .statistics:
                    StatisticsView(viewModel: viewModel)
                case .help:
                    HelpView(viewModel: viewModel)
                case .settings:
                    SettingsView(viewModel: viewModel)
                default:
                    GameView(viewModel: viewModel)
                }
            }
        }
    }
}
