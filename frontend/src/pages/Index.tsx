import React, { useEffect, useState } from 'react';
import { useConfig } from '../hooks';
import { useTranslation } from 'react-i18next';

interface Bank {
  issuerID: string;
  issuerName: string;
}

export default function IndexPage() {
  const config = useConfig();
  const { t, i18n } = useTranslation();
  const [banks, setBanks] = useState<Bank[]>([]);
  const [selected, setSelected] = useState('default');

  useEffect(() => {
    if (!config) return;
    fetch(`${config.idin_server_url}/api/v1/idin/banks`)
      .then((r) => r.json())
      .then((data) => {
        const list: Bank[] = [];
        Object.values(data).forEach((arr: any) => {
          arr.forEach((b: any) => list.push(b));
        });
        setBanks(list);
      });
  }, [config]);

  if (!config) return <p>Loading...</p>;

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    fetch(`${config.idin_server_url}/api/v1/idin/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: selected,
    })
      .then((r) => r.text())
      .then((url) => {
        console.log('Received URL:', url);
        window.location.href = url
      })
      .catch((e) => console.error('start failed', e));
  };

  return (
    <>
      <form id="container" onSubmit={submit}>
        <header>
          <img className="logo-img" src="images/idin-logo.svg" />
          <h1>{t('index_header')} <a href="https://www.idin.nl/" target="_blank">iDIN</a></h1>
        </header>
        <main>
          <div id="idin-form">
            <p>{t('index_explanation')}</p>
            <label htmlFor="bank-select">{t('index_selectbank')}</label>
            <select
              id="bank-select"
              value={selected}
              onChange={(e) => setSelected(e.target.value)}
            >
              <option value="default">{t('index_defaultoption')}</option>
              {banks.map((b) => (
                <option key={b.issuerID} value={b.issuerID}>
                  {b.issuerName}
                </option>
              ))}
            </select>
          </div>
        </main>
        <footer>
          <div className="actions">
            <div></div>
            <button id="submit-button" type="submit" disabled={selected === 'default'}>{t('index_start')}</button>
          </div>
        </footer>
      </form>
    </>
  );
}
