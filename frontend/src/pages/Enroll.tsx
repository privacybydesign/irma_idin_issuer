import React, { useState, useEffect } from 'react';
import Cookies from 'js-cookie';
import jwtDecode from 'jwt-decode';
import { useConfig, useStrings } from '../hooks';
import { Strings } from '../types';

interface CredAttr {
  [key: string]: string;
}

export default function EnrollPage() {
  const config = useConfig();
  const strings = useStrings(config?.language);
  const [idinAttrs, setIdinAttrs] = useState<CredAttr>({});
  const [ageAttrs, setAgeAttrs] = useState<CredAttr>({});

  useEffect(() => {
    const token = Cookies.get('jwt');
    if (token) {
      const decoded: any = jwtDecode(token);
      decoded.iprequest.request.credentials.forEach((cred: any) => {
        if (cred.credential === config?.idin_credential_id) {
          const idin: CredAttr = {};
          const age: CredAttr = {};
          Object.entries(cred.attributes).forEach(([k, v]) => {
            if (k.includes('over')) age[k] = v as string;
            else idin[k] = v as string;
          });
          setIdinAttrs(idin);
          setAgeAttrs(age);
        }
      });
    }
  }, [config]);

  if (!config || !strings) return <p>Loading...</p>;

  const enroll = () => {
    (window as any).yivi
      .newPopup({
        language: config.language,
        session: {
          url: config.irma_server_url,
          start: {
            method: 'POST',
            headers: { 'Content-Type': 'text/plain' },
            body: Cookies.get('jwt')!,
          },
          result: false,
        },
      })
      .start()
      .then(() => {
        window.location.href = '/done';
      });
  };

  return (
    <main className="content">
      <table className="table">
        <tbody>
          {Object.entries(idinAttrs).map(([k, v]) => (
            <tr key={k}>
              <th>{strings[`attribute_${k}`] || k}</th>
              <td>{v}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <table className="table">
        <tbody>
          {Object.entries(ageAttrs).map(([k, v]) => (
            <tr key={k}>
              <th>{strings[`attribute_${k}`] || k}</th>
              <td>{v}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <button id="enroll" className="yivi-web-button" onClick={enroll}>
        {strings.enroll_load_button}
      </button>
    </main>
  );
}
