import { useState, useEffect } from 'react';
import Cookies from 'js-cookie';
import jwtDecode from 'jwt-decode';
import { useConfig } from '../hooks';
import { useTranslation } from 'react-i18next';
import { useNavigate } from "react-router-dom";

interface CredAttr {
  [key: string]: string;
}

export default function EnrollPage() {
  const config = useConfig();
  const navigate = useNavigate();
  const [idinAttrs, setIdinAttrs] = useState<CredAttr>({});
  const [ageAttrs, setAgeAttrs] = useState<CredAttr>({});
  const { t } = useTranslation();

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

  if (!config) return <p>Loading...</p>;

  const enroll = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    import("@privacybydesign/yivi-frontend").then((yivi) => {
      yivi.newPopup({
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
        navigate("/");
      });
    });
  };

  return (
    <>
      <form id="container" onSubmit={enroll}>
        <header>
          <img className="logo-img" src="images/idin-logo.svg" />
          <h1>{t('index_header')} <a href="https://www.idin.nl/" target="_blank">iDIN</a></h1>
        </header>
        <main>
          <div id="idin-form">

            <p>{t('enroll_received_attributes')}</p>
            <table className="table">
              <tbody>
                {Object.entries(idinAttrs).map(([k, v]) => (
                  <tr key={k}>
                    <th>{t(`attribute_${k}`) || k}</th>
                    <td>{v}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            <p>{t('enroll_derived_attributes')}</p>
            <table className="table">
              <tbody>
                {Object.entries(ageAttrs).map(([k, v]) => (
                  <tr key={k}>
                    <th>{t(`attribute_${k}`) || k}</th>
                    <td>{v}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </main>
        <footer>
          <div className="actions">
            <div></div>
            <button id="submit-button" >{t('enroll_load_button')}</button>
          </div>
        </footer>
      </form>
    </>
  );
}
