// import React from "react"
import { ThemeProvider } from "@/stores/theme-provider"
import { RouterProvider } from "react-router-dom"
import dayjs from "dayjs"
import utc from "dayjs/plugin/utc"
import timezone from "dayjs/plugin/timezone"
import router from "@/router"
import "@/main.css"

dayjs.extend(utc)
dayjs.extend(timezone)


export default function Main() {

    return <ThemeProvider>
                <RouterProvider router={router}/>
            </ThemeProvider>;

    // return <React.StrictMode>
    // <ThemeProvider>
    //     <RouterProvider router={router}/>
    //     <Toaster/>
    // </ThemeProvider>
    // </React.StrictMode>;
}