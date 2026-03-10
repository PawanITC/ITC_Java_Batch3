import {APP_NAME} from "../config/env.ts";

export default function Home() {

    return (

        <div style={{ padding: "40px", textAlign: "center" }}>

            <h1>Welcome to {APP_NAME}</h1>

            <p>
                Discover and share creative artwork.
            </p>

        </div>

    );

}