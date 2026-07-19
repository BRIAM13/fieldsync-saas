import React from 'react';
import { SafeAreaView, StyleSheet } from 'react-native';
import TrackingScreen from './src/screens/TrackingScreen';

/** Punto de entrada de la app del cliente. */
export default function App() {
  return (
    <SafeAreaView style={styles.root}>
      <TrackingScreen />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#0f172a' },
});
