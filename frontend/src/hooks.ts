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

