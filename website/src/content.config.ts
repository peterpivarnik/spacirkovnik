import { defineCollection } from 'astro:content';
import { glob } from 'astro/loaders';
import { z } from 'astro/zod';

const blog = defineCollection({
	// Load Markdown and MDX files in the `src/content/blog/` directory.
	loader: glob({ base: './src/content/blog', pattern: '**/*.{md,mdx}' }),
	// Type-check frontmatter using a schema
	schema: z.object({
		title: z.string(),
		description: z.string(),
		// Transform string to Date object
		pubDate: z.coerce.date(),
		updatedDate: z.coerce.date().optional(),
		heroImage: z.string().optional(),
		// Present only on posts that describe a špacírka. Such a post doubles as
		// the game's landing page: it renders the route info box, the download CTA
		// and TouristAttraction structured data, and it feeds the /spacirky list.
		spacirka: z
			.object({
				nazov: z.string(),
				poradie: z.number(),
				region: z.string(),
				dlzka: z.string(),
				cas: z.string(),
				start: z.string(),
				lat: z.number(),
				lng: z.number(),
				paid: z.boolean(),
			})
			.optional(),
	}),
});

export const collections = { blog };
