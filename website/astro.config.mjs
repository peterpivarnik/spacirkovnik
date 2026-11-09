// @ts-check

import mdx from '@astrojs/mdx';
import sitemap from '@astrojs/sitemap';
import { defineConfig, fontProviders } from 'astro/config';

// https://astro.build/config
export default defineConfig({
	site: 'https://spacirkovnik.sk',
	redirects: {
		'/blog/vitajte-v-spacirkovniku': '/blog/vitajte-na-spacirkovniku',
		// Slug renamed to drop the diacritic (it was percent-encoded in the URL).
		'/blog/lesnicka-palica-prvá-hra': '/blog/lesnicka-palica',
		'/blog/lesnicka-palica-prv%C3%A1-hra': '/blog/lesnicka-palica',
		'/hry': '/spacirky',
	},
	integrations: [mdx(), sitemap()],
	fonts: [
		{
			provider: fontProviders.local(),
			name: 'Atkinson',
			cssVariable: '--font-atkinson',
			fallbacks: ['sans-serif'],
			options: {
				variants: [
					{
						src: ['./src/assets/fonts/atkinson-regular.woff'],
						weight: 400,
						style: 'normal',
						display: 'swap',
					},
					{
						src: ['./src/assets/fonts/atkinson-bold.woff'],
						weight: 700,
						style: 'normal',
						display: 'swap',
					},
				],
			},
		},
	],
});
