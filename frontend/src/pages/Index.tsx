import React, { useEffect, useState } from 'react';
import { useConfig } from '../hooks';
import { useTranslation } from 'react-i18next';

interface Bank {
  issuerID: string;
  issuerName: string;
}

export default function IndexPage() {
  const config = useConfig();
  const { t } = useTranslation();
  const [banks, setBanks] = useState<Bank[]>([]);
  const [selected, setSelected] = useState('default');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!config) return;
    fetch(`${config.idin_server_url}/api/v1/idin/banks`)
      .then((r) => {
        if (!r.ok) throw new Error(`banks fetch failed: ${r.status}`);
        return r.json();
      })
      .then((data) => {
        // data is meestal { "Nederland": [ {issuerID, issuerName}, ... ] }
        const list: Bank[] = [];
        Object.values(data).forEach((arr: any) => {
          (arr as any[]).forEach((b: any) => list.push(b));
        });
        setBanks(list);
      })
      .catch((e) => setError(e.message));
  }, [config]);

  if (!config) return <p>Loading...</p>;

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    const body = new URLSearchParams({ bank: selected });

    fetch(`${config.idin_server_url}/api/v1/idin/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: body.toString(),
    })
      .then(async (r) => {
        // Server stuurt JSON terug (succes of fout)
        const data = await r.json().catch(() => ({}));
        if (!r.ok) {
          // Niet-2xx met JSON body (status/description)
          const msg =
            (data && (data.description || data.message)) ||
            `Request failed: ${r.status}`;
          throw new Error(msg);
        }
        if (data?.redirectUrl && /^https?:\/\//i.test(data.redirectUrl)) {
          console.log('trxid:', data.trxid);
          window.location.assign(data.redirectUrl);
          return;
        }
        throw new Error(
          data?.description || 'Ontvangen response bevat geen geldige redirectUrl.'
        );
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  return (
    <>
      <form id="container" onSubmit={submit}>
        <header>
          <img className="logo-img" src="images/idin-logo.svg" />
          <h1>
            {t('index_header')}{' '}
            <a href="https://www.idin.nl/" target="_blank" rel="noreferrer">
              iDIN
            </a>
          </h1>
        </header>
        <main>
          <div id="idin-form">
            <p>{t('index_explanation')}</p>

            {error && (
              <div
                role="alert"
                style={{
                  margin: '0.75rem 0',
                  padding: '0.75rem',
                  border: '1px solid #e11',
                  borderRadius: 8,
                }}
              >
                {error}
              </div>
            )}

            <label htmlFor="bank-select">{t('index_selectbank')}</label>
            <select
              id="bank-select"
              value={selected}
              onChange={(e) => setSelected(e.target.value)}
              disabled={loading}
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
            <button
              id="submit-button"
              type="submit"
              disabled={selected === 'default' || loading}
            >
              {loading ? t('loading') ?? 'Bezig…' : t('index_start')}
            </button>
          </div>
        </footer>
      </form>
    </>
  );
}