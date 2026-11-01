const DASH = /\s*[—–]\s*|\s+-\s+/g;
const SENTENCE_END = /(?<=[.!?…]["“”»']?)\s+(?=["„“«]?[A-ZÄÖÜ])/;
const PARAGRAPH_LENGTH = 240;

/**
 * Splits narration into readable paragraphs. Text without line breaks (older games, or models
 * that ignore the instruction) is grouped by sentences instead.
 */
export function toParagraphs(text: string): string[] {
  const cleaned = text.replace(DASH, ', ').trim();
  const given = cleaned.split(/\n+/).map((part) => part.trim()).filter(Boolean);
  if (given.length > 1 || cleaned.length <= PARAGRAPH_LENGTH) {
    return given;
  }
  const paragraphs: string[] = [];
  let current = '';
  for (const sentence of cleaned.split(SENTENCE_END)) {
    if (current && current.length + sentence.length > PARAGRAPH_LENGTH) {
      paragraphs.push(current);
      current = sentence;
    } else {
      current = current ? `${current} ${sentence}` : sentence;
    }
  }
  if (current) {
    paragraphs.push(current);
  }
  return paragraphs;
}
