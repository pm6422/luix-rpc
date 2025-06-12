import { createRoot } from "react-dom/client"
import { createElement } from "react"
import Main from "./src/main"

  createRoot(document.getElementById("root")!)
    .render(createElement(Main))
