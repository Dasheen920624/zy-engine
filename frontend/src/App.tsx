import { useRoutes } from "react-router-dom";
import { routes } from "./router/routes";

export default function App() {
  const element = useRoutes(routes);
  return <div data-product="factory">{element}</div>;
}
