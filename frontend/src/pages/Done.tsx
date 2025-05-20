import React from 'react';
import { useConfig, useStrings } from '../hooks';
import Cookies from 'js-cookie';

export default function DonePage() {
  const config = useConfig();
  const strings = useStrings(config?.language);

  if (!config || !strings) return <p>Loading...</p>;

  const verify = () => {
    fetch(`${config.idin_server_url}/api/v1/idin/verify`)
      .then((r) => r.text())
      .then((token) => {
        (window as any).yivi
          .newPopup({
            language: config.language,
            session: {
              url: config.irma_server_url,
              start: {
                method: 'POST',
                headers: { 'Content-Type': 'text/plain' },
                body: token,
              },
              result: {
                url: (o: any, { sessionToken }: any) => `${o.url}/session/${sessionToken}/getproof`,
                parseResponse: (r: Response) => r.text(),
              },
            },
          })
          .start();
      });
  };

  return (
    <main className="content">
      <p>{strings.done_text1}</p>
      <p>{strings.done_text2}</p>
      <p>{strings.done_text3}</p>
      <button id="verify_idin_bd_btn" className="yivi-web-button" onClick={verify}>
        {strings.done_show_button}
      </button>
      <a href="/">{strings.done_return}</a>
    </main>
  );
}
