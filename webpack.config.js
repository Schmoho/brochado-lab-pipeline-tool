const MiniCssExtractPlugin = require("mini-css-extract-plugin");

module.exports = {    
    plugins: [
	new MiniCssExtractPlugin({ filename: "standalone/alignmentviewer.css" }),
    ],
    module: {
	rules: [
	    {
		test: /\.(sa|sc|c)ss$/,
		use: [
		    MiniCssExtractPlugin.loader,
		    {
			loader: "css-loader",
			options: {
			    modules: {
				//needed to remove all :global tags, but not generate unique
				//css module names
				getLocalIdent: (
				    context,
				    localIdentName,
				    localName,
				    options
				) => {
				    return localName;
				},
			    },
			},
		    },
		    "sass-loader",
		],
	    },
	],
    },
};
