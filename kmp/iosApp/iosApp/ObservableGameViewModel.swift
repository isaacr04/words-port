import SwiftUI
import shared
import Combine

@MainActor
class ObservableGameViewModel: ObservableObject {
    @Published var state: GameState

    private let viewModel: GameViewModel
    private var cancellable: AnyCancellable?

    init() {
        // Create repositories
        let wordListRepository = InMemoryWordListRepository()
        let statisticsRepository = InMemoryStatisticsRepository()

        // Create ViewModel with main dispatcher
        self.viewModel = GameViewModel(
            wordListRepository: wordListRepository,
            statisticsRepository: statisticsRepository,
            coroutineScope: nil  // Will use default scope
        )

        // Initialize state
        self.state = GameState()

        // Observe state changes
        self.cancellable = createPublisher(viewModel.state)
            .receive(on: DispatchQueue.main)
            .sink { [weak self] newState in
                self?.state = newState
            }
    }

    func processIntent(_ intent: GameIntent) {
        viewModel.processIntent(intent: intent)
    }

    private func createPublisher(_ stateFlow: Kotlinx_coroutines_coreStateFlow) -> AnyPublisher<GameState, Never> {
        return Deferred {
            Future { promise in
                // Collect the flow
                stateFlow.collect { state in
                    if let gameState = state as? GameState {
                        promise(.success(gameState))
                    }
                    return KotlinUnit()
                } completionHandler: { error in
                    // Handle completion
                }
            }
        }
        .eraseToAnyPublisher()
    }
}
