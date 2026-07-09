import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';

export default function SuccessPage() {
  const { t, i18n } = useTranslation();

  useEffect(() => {
    document.title = t('success_title');
  }, [i18n.language, t]);

  return (
    <div id="container">
      <header>
        <img className="logo-img" src="images/idin-logo.svg" alt="iDIN" />
        <h1>
          {t('index_header')}{' '}
          <a href="https://www.idin.nl/" target="_blank" rel="noreferrer">
            iDIN
          </a>
        </h1>
      </header>
      <main>
        <div id="idin-form">
          <div className="imageContainer">
            <img src="images/done.png" alt="" />
          </div>
          <p>{t('success_message')}</p>
        </div>
      </main>
      <footer>
        <div className="actions">
          <div></div>
          <button id="submit-button" type="button" onClick={() => window.close()}>
            {t('success_close_button')}
          </button>
        </div>
      </footer>
    </div>
  );
}
