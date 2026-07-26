import { Specialty } from '../models/work-order.model';

/**
 * Infiere la especialidad que necesita una orden a partir de palabras clave en su título.
 * No hay un campo "categoría" en el backend — el título ya describe el problema (p. ej.
 * "Fuga en tubería principal") y es suficiente para sugerir, sin bloquear, al técnico
 * más adecuado en el panel de asignación.
 */
const PLOMERIA_KEYWORDS = [
  'fuga', 'tubería', 'tuberia', 'grifo', 'caño', 'cano', 'desagüe', 'desague',
  'baño', 'bano', 'calentador', 'agua', 'cañería', 'caneria', 'filtración', 'filtracion',
];

const ELECTRICIDAD_KEYWORDS = [
  'eléctrico', 'electrico', 'eléctrica', 'electrica', 'tablero', 'cableado', 'cable',
  'corto circuito', 'cortocircuito', 'luz', 'luces', 'enchufe', 'breaker', 'fusible',
];

function matchesAny(title: string, keywords: string[]): boolean {
  return keywords.some((k) => title.includes(k));
}

export function inferSpecialty(title: string): Specialty {
  const normalized = title.toLowerCase();
  if (matchesAny(normalized, PLOMERIA_KEYWORDS)) return 'PLOMERIA';
  if (matchesAny(normalized, ELECTRICIDAD_KEYWORDS)) return 'ELECTRICIDAD';
  return 'GENERAL';
}
