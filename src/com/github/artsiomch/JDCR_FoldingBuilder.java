package com.github.artsiomch;

import com.github.artsiomch.utils.JDCR_StringUtils;
import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.lang.folding.NamedFoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnnotatedJavaCodeReferenceElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocToken;
import com.intellij.psi.javadoc.PsiInlineDocTag;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.parser.Parser;

import java.util.ArrayList;
import java.util.List;

public class JDCR_FoldingBuilder implements FoldingBuilder {

  private List<FoldingDescriptor> foldingDescriptors;
  private FoldingGroup foldingGroup;

  @NotNull
  @Override
  public FoldingDescriptor[] buildFoldRegions(@NotNull ASTNode node, @NotNull Document document) {
    PsiElement root = node.getPsi();
    foldingDescriptors = new ArrayList<>();

    PsiTreeUtil.findChildrenOfType( root, PsiDocComment.class).forEach( psiDocComment -> {
      foldingGroup = FoldingGroup.newGroup("JDCR " + psiDocComment.getTokenType().toString());

      PsiTreeUtil.findChildrenOfType( psiDocComment, PsiDocToken.class).forEach( psiDocToken -> {
        JDCR_StringUtils.getCombinedHtmlTags( psiDocToken.getText()).forEach(textRange -> {
          if ( textRange.substring( psiDocToken.getText()).toLowerCase().contains("<li>") )
            addFoldingDescriptor(psiDocToken, textRange, " - ");
          else
            addFoldingDescriptor(psiDocToken, textRange);
        });
        JDCR_StringUtils.getCombinedHtmlEscapedChars( psiDocToken.getText()).forEach(textRange ->
                addFoldingDescriptor( psiDocToken, textRange,
                        Parser.unescapeEntities( textRange.substring( psiDocToken.getText()), true)
                ));
      });

      PsiTreeUtil.findChildrenOfType( psiDocComment, PsiInlineDocTag.class).forEach( psiInlineDocTag -> {
        if (psiInlineDocTag.getName().equals("code")) {
          TextRange textRangeTagStart = new TextRange( 0, 6);
          TextRange textRangeTagEnd = new TextRange( psiInlineDocTag.getTextLength() - 1, psiInlineDocTag.getTextLength());
          addFoldingDescriptor( psiInlineDocTag, textRangeTagStart);
          addFoldingDescriptor( psiInlineDocTag, textRangeTagEnd);
        } else if (psiInlineDocTag.getName().equals("link")) {
          PsiElement psiDocLink = psiInlineDocTag.getValueElement(); // PsiTreeUtil.findChildOfType( psiInlineDocTag, PsiDocMethodOrFieldRef.class);
          if (psiDocLink==null) psiDocLink = PsiTreeUtil.findChildOfType( psiInlineDocTag, PsiAnnotatedJavaCodeReferenceElement.class);
          if (psiDocLink!=null) {
            TextRange textRangeTagStart = new TextRange( 0, 6);// psiDocLink.getTextRange().getStartOffset() - psiInlineDocTag.getTextOffset());
            TextRange textRangeTagEnd = new TextRange( //7
                    psiDocLink.getTextRange().getStartOffset() - psiInlineDocTag.getTextRange().getStartOffset() + psiDocLink.getTextLength(),
                    psiInlineDocTag.getTextLength());
            addFoldingDescriptor( psiInlineDocTag, textRangeTagStart);
            addFoldingDescriptor( psiInlineDocTag, textRangeTagEnd);
          }
        }
      });
    });

    return foldingDescriptors.toArray(new FoldingDescriptor[0]);
  }

  private void addFoldingDescriptor(PsiElement element, TextRange range) {
    addFoldingDescriptor( element, range, ""); //"◊"
  }
  private void addFoldingDescriptor(PsiElement element, TextRange range, String placeholderText) {
    foldingDescriptors.add( new NamedFoldingDescriptor(
            element.getNode(),
            range.shiftRight( element.getTextRange().getStartOffset()),
            foldingGroup,
            placeholderText
    ));
  }

  @Nullable
  @Override
  public String getPlaceholderText(@NotNull ASTNode node) {
    return null;
  }

  @Override
  public boolean isCollapsedByDefault(@NotNull ASTNode node) {
    return true;
  }
}
