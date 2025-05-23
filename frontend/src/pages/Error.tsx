import { useConfig } from '../hooks';
import Cookies from 'js-cookie';
import { useTranslation } from 'react-i18next';

export default function ErrorPage() {
  const config = useConfig();
  const { t } = useTranslation();


  if (!config) return <p>Loading...</p>;

  const error = Cookies.get('error');
  return (
    <main className="content">
      <p>{error}</p>
      <a href="/">{t('error_text2')}</a>
    </main>
  );
}
