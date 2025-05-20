import React from 'react';
import { useConfig, useStrings } from '../hooks';
import Cookies from 'js-cookie';

export default function ErrorPage() {
  const config = useConfig();
  const strings = useStrings(config?.language);

  if (!config || !strings) return <p>Loading...</p>;

  const error = Cookies.get('error');
  return (
    <main className="content">
      <p>{error}</p>
      <a href="/">{strings.error_text2}</a>
    </main>
  );
}
