import React, { useEffect, useState } from 'react';
import { useConfig, useStrings } from '../hooks';
import { Config, Strings } from '../types';

interface Bank {
  issuerID: string;
  issuerName: string;
}

export default function IndexPage() {
  const config = useConfig();
  const strings = useStrings(config?.language);
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

  if (!config || !strings) return <p>Loading...</p>;

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    fetch(`${config.idin_server_url}/api/v1/idin/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: selected,
    })
      .then((r) => r.text())
      .then((url) => (window.location.href = url))
      .catch((e) => console.error('start failed', e));
  };

  return (
    <main className="content">
      <form onSubmit={submit} id="form">
        <label htmlFor="bank-select">{strings.index_selectbank}</label>
        <select
          id="bank-select"
          value={selected}
          onChange={(e) => setSelected(e.target.value)}
        >
          <option value="default">{strings.index_defaultoption}</option>
          {banks.map((b) => (
            <option key={b.issuerID} value={b.issuerID}>
              {b.issuerName}
            </option>
          ))}
        </select>
        <button className="yivi-web-button" disabled={selected === 'default'}>
          Ga naar uw bank
        </button>
      </form>
    </main>
  );
}
