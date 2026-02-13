import { useState, useEffect } from "react";
import Header from "./components/Header";
import Search from "./components/Search";
import CurrentWeather from "./components/CurrentWeather";
import Metrics from "./components/Metrics";
import DailyForecast from "./components/DailyForecast";
import HourlyForecast from "./components/HourlyForecast";
import LoadingState from "./components/LoadingState";
import { fetchWeatherData } from "./lib/utils";
import error from "./asets/images/icon-error.svg";
import retry from "./assets/images/icon-retry.svg";




export default function App(){
  return <div className="text-4xl font-bold"> Apptest  </div>
}