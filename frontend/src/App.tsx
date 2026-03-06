import RandomComponent from "./components/randomComponent.tsx";

function App() {

    return (
        <>
            <div>
                <h1>Root</h1>
                <h2>Main.tsx gets the root element and renders the root to contain an App component</h2>
                <h3>The app file that is rendered</h3>
            </div>
            <div>
                <RandomComponent/>
            </div>
        </>
    )
}

export default App
