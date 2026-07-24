import { registerRootComponent } from 'expo';
import App from './App';

// registerRootComponent llama a AppRegistry.registerComponent('main', () => App) y además
// configura el entorno correcto ya sea que la app corra en Expo Go, un build nativo, o web.
registerRootComponent(App);
