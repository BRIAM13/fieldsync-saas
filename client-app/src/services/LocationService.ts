import * as Location from 'expo-location';

export interface Coordinates {
  lat: number;
  lng: number;
}

/**
 * Pide permiso de ubicación y devuelve la posición actual del cliente. Necesaria para que
 * la solicitud de servicio tenga `location` real y aparezca como pin en el mapa de despacho.
 */
export async function requestAndGetCurrentLocation(): Promise<Coordinates> {
  const { status } = await Location.requestForegroundPermissionsAsync();
  if (status !== 'granted') {
    throw new Error('Necesitamos tu ubicación para enviar la solicitud. Actívala en Ajustes.');
  }
  const position = await Location.getCurrentPositionAsync({});
  return { lat: position.coords.latitude, lng: position.coords.longitude };
}
