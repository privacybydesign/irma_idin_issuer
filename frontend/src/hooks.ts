import { useEffect, useState } from 'react';
import { Config, Strings } from './types';

export function useConfig(): Config | null {
  const [config, setConfig] = useState<Config | null>(null);
  useEffect(() => {
    fetch('/conf.json')
      .then((r) => r.json())
      .then(setConfig)
      .catch((e) => console.error('config fetch failed', e));
  }, []);
  return config;
}

export function useStrings(language?: string): Strings | null {
  const [strings, setStrings] = useState<Strings | null>(null);
  useEffect(() => {
    if (!language) return;
    fetch(`/languages/${language}.json`)
      .then((r) => r.json())
      .then(setStrings)
      .catch((e) => console.error('strings fetch failed', e));
  }, [language]);
  return strings;
}
