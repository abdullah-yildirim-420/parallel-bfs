import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import { connectLiveStream } from "./store";
import "./index.css";

connectLiveStream();

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
