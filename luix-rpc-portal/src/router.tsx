import { createBrowserRouter } from "react-router-dom"

const router = createBrowserRouter([
  // account-related routes
  {
    path: "/",
    lazy: async () => ({
      Component: (await import("./views")).default
    }),
  },
])

export default router
